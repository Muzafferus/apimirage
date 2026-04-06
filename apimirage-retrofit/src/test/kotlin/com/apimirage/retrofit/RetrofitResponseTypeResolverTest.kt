package com.apimirage.retrofit

import com.apimirage.core.generation.ApiMirageTypeKey
import java.lang.reflect.Proxy
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Invocation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

public class RetrofitResponseTypeResolverTest {
    private val resolver = RetrofitResponseTypeResolver()

    @Test
    public fun `resolver unwraps Call body types`() {
        val result = resolveRequest("user", listOf("7")).success()

        assertEquals(RetrofitResponseDeclaration.CALL, result.resolvedType.declaration)
        assertType(result.resolvedType.bodyType, UserDto::class.java.name, emptyList())
    }

    @Test
    public fun `resolver preserves list body types`() {
        val result = resolveRequest("users").success()

        assertEquals(RetrofitResponseDeclaration.CALL, result.resolvedType.declaration)
        assertType(
            typeKey = result.resolvedType.bodyType,
            expectedRawTypeName = List::class.java.name,
            expectedArguments = listOf(UserDto::class.java.name),
        )
    }

    @Test
    public fun `resolver preserves nested wrapper body types`() {
        val result = resolveRequest("wrappedUser").success()

        assertEquals(RetrofitResponseDeclaration.CALL, result.resolvedType.declaration)
        assertType(
            typeKey = result.resolvedType.bodyType,
            expectedRawTypeName = BaseResponse::class.java.name,
            expectedArguments = listOf(UserDto::class.java.name),
        )
    }

    @Test
    public fun `resolver unwraps direct Response body types`() {
        val result = resolveMethod("directResponse").success()

        assertEquals(RetrofitResponseDeclaration.RESPONSE, result.resolvedType.declaration)
        assertType(result.resolvedType.bodyType, UserDto::class.java.name, emptyList())
    }

    @Test
    public fun `resolver handles suspend body returns`() {
        val bodyResult = resolveMethod("suspendUser").success()
        val listResult = resolveMethod("suspendUsers").success()
        val wrappedResult = resolveMethod("suspendWrappedUser").success()

        assertEquals(RetrofitResponseDeclaration.SUSPEND_BODY, bodyResult.resolvedType.declaration)
        assertType(bodyResult.resolvedType.bodyType, UserDto::class.java.name, emptyList())

        assertEquals(RetrofitResponseDeclaration.SUSPEND_BODY, listResult.resolvedType.declaration)
        assertType(
            typeKey = listResult.resolvedType.bodyType,
            expectedRawTypeName = List::class.java.name,
            expectedArguments = listOf(UserDto::class.java.name),
        )

        assertEquals(RetrofitResponseDeclaration.SUSPEND_BODY, wrappedResult.resolvedType.declaration)
        assertType(
            typeKey = wrappedResult.resolvedType.bodyType,
            expectedRawTypeName = BaseResponse::class.java.name,
            expectedArguments = listOf(UserDto::class.java.name),
        )
    }

    @Test
    public fun `resolver handles suspend Response returns`() {
        val result = resolveMethod("suspendResponse").success()

        assertEquals(RetrofitResponseDeclaration.SUSPEND_RESPONSE, result.resolvedType.declaration)
        assertType(result.resolvedType.bodyType, UserDto::class.java.name, emptyList())
    }

    @Test
    public fun `resolver reports unsupported when request has no invocation tag`() {
        val request = Request.Builder()
            .url("https://example.com/missing")
            .build()

        val result = resolver.resolve(request)

        assertTrue(result is RetrofitResponseTypeResolution.Unsupported)
    }

    private fun resolveRequest(
        methodName: String,
        arguments: List<Any?> = emptyList(),
    ): RetrofitResponseTypeResolution.Success {
        val method = SampleService::class.java.methods.single { it.name == methodName }
        val request = Request.Builder()
            .url("https://example.com/$methodName")
            .tag(Invocation::class.java, invocationFor(method, arguments))
            .build()

        return resolver.resolve(request).success()
    }

    private fun resolveMethod(methodName: String): RetrofitResponseTypeResolution.Success {
        val method = SampleService::class.java.methods.single { it.name == methodName }
        return resolver.resolve(invocationFor(method, emptyList())).success()
    }

    private fun invocationFor(
        method: java.lang.reflect.Method,
        arguments: List<Any?>,
    ): Invocation {
        val instance = Proxy.newProxyInstance(
            SampleService::class.java.classLoader,
            arrayOf(SampleService::class.java),
        ) { _, _, _ -> null } as SampleService

        return Invocation.of(SampleService::class.java, instance, method, arguments)
    }

    private fun RetrofitResponseTypeResolution.success(): RetrofitResponseTypeResolution.Success {
        return this as? RetrofitResponseTypeResolution.Success
            ?: error("Expected success but was $this")
    }

    private fun assertType(
        typeKey: ApiMirageTypeKey,
        expectedRawTypeName: String,
        expectedArguments: List<String>,
    ) {
        val actualRawTypeName = typeKey.rawClass?.java?.name ?: typeKey.rawTypeName
        assertEquals(expectedRawTypeName, actualRawTypeName)
        assertEquals(expectedArguments, typeKey.arguments.map { it.rawClass?.java?.name ?: it.rawTypeName })
    }

    private interface SampleService {
        @GET("users/{id}")
        fun user(@Path("id") id: String): Call<UserDto>

        @GET("users")
        fun users(): Call<List<UserDto>>

        @GET("wrapped-user")
        fun wrappedUser(): Call<BaseResponse<UserDto>>

        @GET("direct-response")
        fun directResponse(): Response<UserDto>

        @GET("suspend-user")
        suspend fun suspendUser(): UserDto

        @GET("suspend-users")
        suspend fun suspendUsers(): List<UserDto>

        @GET("suspend-wrapped-user")
        suspend fun suspendWrappedUser(): BaseResponse<UserDto>

        @GET("suspend-response")
        suspend fun suspendResponse(): Response<UserDto>
    }

    private data class UserDto(
        val id: Long,
        val name: String,
    )

    private data class BaseResponse<T>(
        val data: T,
        val success: Boolean,
    )
}
