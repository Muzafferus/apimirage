# ApiMirage

ApiMirage is a Kotlin-first Android mocking library for Retrofit apps.

When ApiMirage is enabled, it intercepts outgoing Retrofit requests inside the app process, resolves the declared response type, generates a realistic mock object, serializes it to JSON, and returns a synthetic HTTP 200 response so Retrofit parses it as if it came from the backend.

The MVP keeps the public API intentionally small:

```kotlin
ApiMirage.install(enabled = BuildConfig.DEBUG)
```

or:

```kotlin
OkHttpClient.Builder()
    .addInterceptor(ApiMirageInterceptor(enabled = BuildConfig.DEBUG))
    .build()
```

If ApiMirage is disabled, requests pass through to the real network with no behavior change.

## Status

- Retrofit is the only supported adapter in v1.
- Debug builds default to ON.
- Release builds default to OFF.
- No external mock server is required.
- Deterministic mock data is supported with a seed.

## Module Structure

| Module | Purpose |
| --- | --- |
| `:apimirage-core` | Adapter-agnostic config, generation engine, fake value providers, seed support, annotations, and extension hooks. |
| `:apimirage-retrofit` | Retrofit integration through an OkHttp application interceptor, `Invocation` inspection, response type resolution, diagnostics, and synthetic HTTP response creation. |
| `:sample-app` | Small demo app that proves `UserDto`, `List<UserDto>`, and `BaseResponse<UserDto>` flows end to end. |

The structure is designed so future adapters can live beside Retrofit without pulling generation logic out of `:apimirage-core`.

Possible future modules:

- `:apimirage-ktor`
- `:apimirage-apollo`

## How It Works

1. `ApiMirageInterceptor` runs as an OkHttp application interceptor.
2. It reads Retrofit metadata from `request.tag(Invocation::class.java)`.
3. It resolves the real body type behind declarations such as `Call<T>`, `Response<T>`, and suspend functions.
4. It generates a realistic object for that resolved type.
5. It serializes the generated object to JSON, preferring `kotlinx.serialization`.
6. It returns a synthetic `okhttp3.Response` with HTTP 200 and `application/json`.
7. Retrofit keeps using its normal converter stack, so parsing behavior stays realistic.

## Quick Start

Right now ApiMirage is set up as a multi-module project, so the simplest integration is a local module dependency.

```kotlin
dependencies {
    implementation(project(":apimirage-retrofit"))
}
```

`:apimirage-retrofit` already depends on `:apimirage-core`, so most apps only need the Retrofit module directly.

Install ApiMirage once during app startup:

```kotlin
import com.apimirage.core.ApiMirage

ApiMirage.install(enabled = BuildConfig.DEBUG)
```

Add the interceptor to the same `OkHttpClient` used by Retrofit:

```kotlin
import com.apimirage.retrofit.ApiMirageInterceptor

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(ApiMirageInterceptor())
    .build()
```

Build Retrofit as usual:

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
```

That is enough for Retrofit requests to be auto-mocked when ApiMirage is enabled.

## Tiny Config API

Use the boolean shortcut when all you need is on or off:

```kotlin
ApiMirage.install(enabled = BuildConfig.DEBUG)
```

Use `ApiMirageConfig` when you want deterministic data or debug diagnostics:

```kotlin
import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics

ApiMirage.install(
    ApiMirageConfig(
        enabled = BuildConfig.DEBUG,
        seed = 20260407L,
        diagnostics = if (BuildConfig.DEBUG) {
            ApiMirageDiagnostics.LOGS
        } else {
            ApiMirageDiagnostics.NONE
        },
    ),
)
```

Defaults:

- Debug variant: `enabled = true`, `diagnostics = LOGS`
- Release variant: `enabled = false`, `diagnostics = NONE`

## Supported Retrofit Declarations in v1

ApiMirage currently resolves and mocks these Retrofit response shapes:

- `Call<T>`
- `Response<T>`
- `suspend fun ... : T`
- `suspend fun ... : Response<T>`
- `List<T>`
- nested wrappers such as `BaseResponse<T>`

The resolved body type is what gets generated and serialized.

## Supported Model Shapes in v1

The generator currently supports:

- primitives
- `String`
- `Boolean`
- enums
- nullable fields
- Kotlin data classes
- nested Kotlin data classes
- lists
- maps with JSON-safe keys
- generic wrapper models such as `BaseResponse<UserDto>`
- common date/time string fields

Field-name heuristics currently produce sensible fake values for names such as:

- `id`
- `name`
- `email`
- `phone`
- `url`
- `title`
- `description`
- `createdAt`
- `updatedAt`

Typical outputs include:

- numeric IDs for numeric `id` fields
- stable-looking string IDs for string `id` fields
- full names like `Avery Nguyen`
- emails like `avery.nguyen42@example.com`
- URLs like `https://example.com/profile/abc123`
- ISO-8601 timestamps for `createdAt` and `updatedAt`

List generation returns at least 3 items in the MVP.

## Serialization Strategy

ApiMirage uses a two-step JSON encoding strategy:

1. Try `kotlinx.serialization` first.
2. If no serializer is available, fall back to a reflective `JsonElement` encoder for supported v1 shapes.

This keeps the happy path Kotlin-first while still handling common data-class models without forcing every DTO to be annotated.

## Deterministic Seed Support

When a seed is provided, mock data is deterministic.

```kotlin
ApiMirage.install(
    ApiMirageConfig(
        enabled = true,
        seed = 1234L,
    ),
)
```

Current behavior:

- the same seed produces the same data across runs
- the same seed and the same endpoint produce the same payload
- different endpoints still diverge because ApiMirage forks the random source using stable endpoint metadata

This is useful for repeatable UI screenshots, snapshot tests, QA flows, and debugging.

## Debug Logging and Fallback Behavior

Set `diagnostics = ApiMirageDiagnostics.LOGS` to see what ApiMirage is doing.

Current logs include:

- which endpoint was intercepted
- which response model was resolved
- whether a mock response was returned
- whether pass-through happened
- why a fallback happened

Typical examples:

```text
[ApiMirage] Resolved response model UserDto for GET https://example.com/user.
[ApiMirage] Mocked GET https://example.com/user as UserDto using KOTLINX_SERIALIZATION.
[ApiMirage] Pass-through GET https://example.com/user because ApiMirage is disabled.
```

Fail-safe behavior in v1:

- if mocking is disabled, ApiMirage always passes through
- if Retrofit `Invocation` metadata is missing, ApiMirage passes through
- if a response type cannot be resolved, ApiMirage passes through
- if generation or encoding cannot safely support a shape, ApiMirage passes through
- when diagnostics are on, ApiMirage logs the reason before passing through

## Endpoint-Level Controls and Extension Hooks

### Opt out of auto-mocking

Use `@NoAutoMock` on a Retrofit service or endpoint:

```kotlin
import com.apimirage.core.annotations.NoAutoMock

interface UserApi {
    @NoAutoMock
    @GET("user/live")
    fun liveUser(): Call<UserDto>
}
```

### Override inferred field hints

Use `@ApiMirageFieldHint` when a property name alone is not enough:

```kotlin
import com.apimirage.core.annotations.ApiMirageFieldHint
import com.apimirage.core.fake.ApiMirageValueHint

data class ContactDto(
    @ApiMirageFieldHint(ApiMirageValueHint.PHONE)
    val supportLine: String,
)
```

### Register a custom fake value provider

```kotlin
import com.apimirage.core.ApiMirage
import com.apimirage.core.fake.ApiMirageFakeValueProvider
import com.apimirage.core.fake.ApiMirageFakeValueRequest
import com.apimirage.core.fake.ApiMirageFakeValueResult

ApiMirage.registerFakeValueProvider(
    ApiMirageFakeValueProvider { request: ApiMirageFakeValueRequest ->
        if (request.property.declaredName == "name") {
            ApiMirageFakeValueResult.Provided("Custom Name")
        } else {
            ApiMirageFakeValueResult.Unhandled
        }
    },
)
```

### Register a per-endpoint override

```kotlin
import com.apimirage.core.ApiMirage
import com.apimirage.core.generation.ApiMirageGenerationResult

ApiMirage.registerEndpointOverride { request ->
    if (request.endpoint.operationName == "user") {
        ApiMirageGenerationResult.Success(
            UserDto(
                id = 700L,
                name = "Override Name",
            ),
        )
    } else {
        null
    }
}
```

Clear registered customizations:

```kotlin
ApiMirage.clearCustomizations()
```

## Sample App

The sample app demonstrates three acceptance flows:

- `Call<UserDto>`
- `Call<List<UserDto>>`
- `Call<BaseResponse<UserDto>>`

The sample installs ApiMirage with a deterministic seed and displays parsed results in a simple on-device UI.

Useful commands:

```bash
./gradlew :sample-app:assembleDebug
./gradlew :sample-app:testDebugUnitTest
```

## Test Coverage

Current tests cover:

- Retrofit response type resolution
- mock generation rules
- deterministic seed behavior
- pass-through behavior when disabled
- interceptor integration with synthetic HTTP 200 responses
- sample-app end-to-end parsing

Useful command:

```bash
./gradlew :apimirage-core:testDebugUnitTest :apimirage-retrofit:testDebugUnitTest :sample-app:testDebugUnitTest
```

## Unsupported or Postponed in v1

These areas are intentionally out of scope for the MVP or only partially handled today:

- Ktor adapter support
- Apollo / GraphQL adapter support
- polymorphic model graphs
- sealed hierarchies and abstract types as primary DTO shapes
- arbitrary interface-based models
- cyclic object graphs
- very deep recursive shapes beyond the current safety limit
- custom Java or Kotlin time object types such as `Instant`, `LocalDate`, or `OffsetDateTime` as direct DTO fields
- array-focused APIs as a primary supported shape
- remote mock servers or proxy infrastructure
- rich per-endpoint DSLs or scenario scripting

If a shape is unsupported, ApiMirage is designed to fail safely by passing through to the real network instead of breaking the request pipeline.

## Roadmap

Near-term improvements:

- publish artifacts for easier external consumption
- expand fake value heuristics and field annotations
- improve diagnostics around unsupported models
- add more per-endpoint override ergonomics
- add instrumentation coverage in the sample app

Future adapter roadmap:

- `:apimirage-ktor` using the same core generation and serialization engine
- `:apimirage-apollo` for GraphQL response mocking
- adapter-specific diagnostics that still share the same `ApiMirageConfig` and extension model

## Design Goals

- tiny public API
- zero behavior change when disabled
- easy to debug in app process
- deterministic when seeded
- easy to extend without over-engineering the MVP
