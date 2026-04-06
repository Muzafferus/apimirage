# ApiMirage

ApiMirage is a Kotlin-first Android library for in-process Retrofit mocking.

It intercepts outgoing Retrofit requests before the real network call, resolves the declared response type, generates realistic mock data, serializes it to JSON, and returns a synthetic HTTP 200 response so Retrofit parses it exactly like a backend response.

No mock server. No proxy. No fake API environment to keep running.

## Why ApiMirage

- Drop-in Retrofit support through an OkHttp application interceptor
- Tiny public API with build-type-aware defaults
- Realistic, non-empty JSON payloads generated inside the app process
- Deterministic fake data when you provide a seed
- Safe fallback to the real network when mocking is disabled or unsupported
- Designed so future adapters can support Ktor and Apollo without rewriting the core engine

## What It Looks Like

```kotlin
ApiMirage.install()

val client = OkHttpClient.Builder()
    .addInterceptor(ApiMirageInterceptor())
    .build()
```

If you want explicit control:

```kotlin
ApiMirage.install(enabled = BuildConfig.DEBUG)
```

or:

```kotlin
ApiMirage.install(
    ApiMirageConfig(
        enabled = BuildConfig.DEBUG,
        seed = 20260407L,
        diagnostics = ApiMirageDiagnostics.LOGS,
    ),
)
```

## Status

ApiMirage is currently an MVP with Retrofit support only.

- Debug builds default to ON
- Release builds default to OFF
- Retrofit requests pass through unchanged when ApiMirage is disabled
- The repository includes a sample app and unit coverage for the core flow
- Publishing to Maven is not set up yet, so current integration is via local modules

## How It Works

1. `ApiMirageInterceptor` runs as an OkHttp application interceptor.
2. It reads Retrofit metadata from `request.tag(Invocation::class.java)`.
3. It resolves the actual body type behind `Call<T>`, `Response<T>`, suspend functions, lists, and wrapper models.
4. It generates a realistic mock object for that resolved type.
5. It serializes the object to JSON, preferring `kotlinx.serialization`.
6. It returns a synthetic `okhttp3.Response` with HTTP 200 and JSON.
7. Retrofit keeps using its normal converters, so your parsing path stays real.

## Module Structure

| Module | Purpose |
| --- | --- |
| `:apimirage-core` | Config, mock generation engine, fake value providers, annotations, deterministic seed support, and extension hooks |
| `:apimirage-retrofit` | OkHttp interceptor, Retrofit `Invocation` reader, response type resolver, diagnostics, and synthetic response creation |
| `:sample-app` | Small demo app proving `UserDto`, `List<UserDto>`, and `BaseResponse<UserDto>` end to end |

This split is intentional: the generation engine lives in `:apimirage-core`, while transport-specific integrations live in adapter modules.

## Quick Start

### 1. Add the module

Right now the project is set up as local modules:

```kotlin
dependencies {
    implementation(project(":apimirage-retrofit"))
}
```

`:apimirage-retrofit` already depends on `:apimirage-core`.

### 2. Install ApiMirage

Use the build-type-aware default:

```kotlin
ApiMirage.install()
```

Or be explicit:

```kotlin
ApiMirage.install(enabled = BuildConfig.DEBUG)
```

### 3. Add the interceptor to OkHttp

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(ApiMirageInterceptor())
    .build()
```

### 4. Build Retrofit as usual

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
```

That is enough to let Retrofit receive synthetic JSON responses when mocking is enabled.

## Public API

The MVP keeps the public API intentionally small.

```kotlin
ApiMirage.install()
ApiMirage.install(enabled = BuildConfig.DEBUG)
ApiMirage.install(ApiMirageConfig(...))
```

`ApiMirageConfig` currently contains:

- `enabled`
- `seed`
- `diagnostics`

Defaults:

- Debug variant: `enabled = true`, `diagnostics = LOGS`
- Release variant: `enabled = false`, `diagnostics = NONE`

## Supported Retrofit Declarations in v1

ApiMirage currently resolves and mocks:

- `Call<T>`
- `Response<T>`
- `suspend fun ... : T`
- `suspend fun ... : Response<T>`
- `List<T>`
- nested wrappers such as `BaseResponse<UserDto>`

The resolved body type is what gets generated and serialized.

## Supported Model Shapes in v1

The generation engine currently supports:

- primitives
- `String`
- `Boolean`
- enums
- nullable fields
- Kotlin data classes
- nested Kotlin data classes
- lists
- maps with JSON-safe keys
- generic wrappers such as `BaseResponse<T>`
- common date/time string fields

Built-in field heuristics cover names such as:

- `id`
- `name`
- `email`
- `phone`
- `url`
- `title`
- `description`
- `createdAt`
- `updatedAt`

Examples of the kind of values ApiMirage emits:

- full names like `Avery Nguyen`
- emails like `avery.nguyen42@example.com`
- URLs like `https://example.com/profile/abc123`
- ISO-8601 timestamps for `createdAt` and `updatedAt`
- numeric IDs for numeric identifier fields

Lists return at least 3 items in the MVP.

## Serialization Strategy

ApiMirage uses a Kotlin-first encoding path:

1. Try `kotlinx.serialization`
2. Fall back to a reflective `JsonElement` encoder for supported data-class shapes
3. If a shape still cannot be encoded safely, pass through to the real network or emit a debug diagnostic

This keeps the happy path clean while still supporting common non-annotated DTOs.

## Deterministic Seed Support

Provide a seed to make generated responses repeatable:

```kotlin
ApiMirage.install(
    ApiMirageConfig(
        enabled = true,
        seed = 1234L,
    ),
)
```

Current behavior:

- the same seed produces the same values across runs
- the same seed and the same endpoint produce the same payload
- different endpoints still diverge because ApiMirage forks the random source using stable endpoint metadata

This is useful for repeatable screenshots, QA flows, UI testing, and debugging.

## Debug Logging and Safe Fallback

Enable logs with:

```kotlin
ApiMirage.install(
    ApiMirageConfig(
        enabled = true,
        diagnostics = ApiMirageDiagnostics.LOGS,
    ),
)
```

Current logs tell you:

- which endpoint was intercepted
- which response model was resolved
- whether a synthetic mock response was returned
- whether the request passed through
- why a fallback happened

Example:

```text
[ApiMirage] Resolved response model UserDto for GET https://example.com/user.
[ApiMirage] Mocked GET https://example.com/user as UserDto using KOTLINX_SERIALIZATION.
[ApiMirage] Pass-through GET https://example.com/user because ApiMirage is disabled.
```

Fail-safe behavior in v1:

- if mocking is disabled, requests always pass through
- if Retrofit `Invocation` metadata is missing, requests pass through
- if response type resolution fails, requests pass through
- if mock generation fails safely, requests pass through
- if JSON encoding fails safely, requests pass through

When diagnostics are enabled, ApiMirage logs the reason before passing through.

## Extension Hooks

The main API stays small, but the core already includes early extension points for future growth.

### `@NoAutoMock`

Opt a service or endpoint out of automatic mocking:

```kotlin
@NoAutoMock
@GET("user/live")
fun liveUser(): Call<UserDto>
```

### `@ApiMirageFieldHint`

Override inferred field semantics when property names are not enough:

```kotlin
data class ContactDto(
    @ApiMirageFieldHint(ApiMirageValueHint.PHONE)
    val supportLine: String,
)
```

### Custom fake value providers

```kotlin
ApiMirage.registerFakeValueProvider(
    ApiMirageFakeValueProvider { request ->
        if (request.property.declaredName == "name") {
            ApiMirageFakeValueResult.Provided("Custom Name")
        } else {
            ApiMirageFakeValueResult.Unhandled
        }
    },
)
```

### Per-endpoint overrides

```kotlin
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

Clear customizations:

```kotlin
ApiMirage.clearCustomizations()
```

## Sample App

The sample app demonstrates the MVP acceptance path with:

- `Call<UserDto>`
- `Call<List<UserDto>>`
- `Call<BaseResponse<UserDto>>`

It installs ApiMirage with a deterministic seed and shows parsed results in a simple Android UI.

Useful commands:

```bash
./gradlew :sample-app:assembleDebug
./gradlew :sample-app:testDebugUnitTest
```

## Tests

Current automated coverage includes:

- Retrofit response type resolution
- mock generation rules
- deterministic seed behavior
- disabled-mode pass-through
- interceptor integration with synthetic HTTP 200 responses
- sample-app end-to-end parsing

Useful command:

```bash
./gradlew :apimirage-core:testDebugUnitTest :apimirage-retrofit:testDebugUnitTest :sample-app:testDebugUnitTest
```

## Unsupported or Postponed in v1

The MVP intentionally does not try to solve everything yet.

Postponed or only partially handled today:

- Ktor adapter support
- Apollo / GraphQL adapter support
- polymorphic DTO graphs
- sealed hierarchies and abstract root models
- arbitrary interface-based response models
- cyclic object graphs
- very deep recursive shapes beyond the current safety limit
- direct support for time objects such as `Instant`, `LocalDate`, or `OffsetDateTime`
- remote mock servers or proxy infrastructure
- rich scenario scripting or endpoint DSLs

If a shape is unsupported, ApiMirage is designed to fail safely and fall back to the real network instead of breaking the request pipeline.

## Roadmap

Near term:

- publish artifacts for easier external consumption
- expand fake value heuristics and field annotations
- improve diagnostics around unsupported shapes
- add more ergonomic endpoint override APIs
- add instrumentation coverage in the sample app

Future adapter roadmap:

- `:apimirage-ktor`
- `:apimirage-apollo`

The goal is to keep one core generation engine and add thin adapter-specific integrations on top.

## Design Goals

- tiny public API
- zero behavior change when disabled
- easy to debug
- deterministic when seeded
- realistic Retrofit parsing path
- no over-engineering for the MVP

