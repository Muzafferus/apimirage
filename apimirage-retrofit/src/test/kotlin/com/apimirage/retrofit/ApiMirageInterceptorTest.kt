package com.apimirage.retrofit

import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.core.fake.ApiMirageFakeValueProvider
import com.apimirage.core.fake.ApiMirageFakeValueRequest
import com.apimirage.core.fake.ApiMirageFakeValueResult
import com.apimirage.core.generation.ApiMirageGenerationResult
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Invocation
import retrofit2.http.GET

public class ApiMirageInterceptorTest {
    @After
    public fun tearDown() {
        ApiMirage.clearCustomizations()
    }

    @Test
    public fun `interceptor returns synthetic 200 JSON when enabled`() {
        val logger = RecordingLogger()
        val interceptor = ApiMirageInterceptor(
            configProvider = {
                ApiMirageConfig(
                    enabled = true,
                    seed = 99L,
                    diagnostics = ApiMirageDiagnostics.LOGS,
                )
            },
            logger = logger,
        )
        val chain = FakeChain(request = taggedRequest("user"))

        val response = interceptor.intercept(chain)
        val body = response.body.string()

        assertEquals(0, chain.proceedCalls)
        assertEquals(200, response.code)
        assertTrue(body.contains("\"id\""))
        assertTrue(body.contains("\"name\""))
        assertTrue(logger.messages.any { it.contains("Resolved response model UserDto") })
        assertTrue(logger.messages.any { it.contains("Mocked GET https://example.com/user as UserDto") })
    }

    @Test
    public fun `disabled interceptor passes through with zero behavior change`() {
        val logger = RecordingLogger()
        val interceptor = ApiMirageInterceptor(
            configProvider = {
                ApiMirageConfig(
                    enabled = false,
                    diagnostics = ApiMirageDiagnostics.LOGS,
                )
            },
            logger = logger,
        )
        val chain = FakeChain(request = taggedRequest("user"))

        val response = interceptor.intercept(chain)

        assertEquals(1, chain.proceedCalls)
        assertEquals(204, response.code)
        assertTrue(logger.messages.any { it.contains("because ApiMirage is disabled") })
    }

    @Test
    public fun `non-retrofit requests pass through and log why`() {
        val logger = RecordingLogger()
        val interceptor = ApiMirageInterceptor(
            configProvider = {
                ApiMirageConfig(
                    enabled = true,
                    diagnostics = ApiMirageDiagnostics.LOGS,
                )
            },
            logger = logger,
        )
        val chain = FakeChain(
            request = Request.Builder()
                .url("https://example.com/plain")
                .get()
                .build(),
        )

        val response = interceptor.intercept(chain)

        assertEquals(1, chain.proceedCalls)
        assertEquals(204, response.code)
        assertTrue(logger.messages.any { it.contains("Invocation metadata is missing") })
    }

    @Test
    public fun `no auto mock endpoints pass through`() {
        val logger = RecordingLogger()
        val interceptor = ApiMirageInterceptor(
            configProvider = {
                ApiMirageConfig(
                    enabled = true,
                    diagnostics = ApiMirageDiagnostics.LOGS,
                )
            },
            logger = logger,
        )
        val chain = FakeChain(request = taggedRequest("realUser"))

        val response = interceptor.intercept(chain)

        assertEquals(1, chain.proceedCalls)
        assertEquals(204, response.code)
        assertTrue(logger.messages.any { it.contains("@NoAutoMock") })
    }

    @Test
    public fun `unsupported response types pass through safely with diagnostics`() {
        val logger = RecordingLogger()
        val interceptor = ApiMirageInterceptor(
            configProvider = {
                ApiMirageConfig(
                    enabled = true,
                    diagnostics = ApiMirageDiagnostics.LOGS,
                )
            },
            logger = logger,
        )
        val chain = FakeChain(request = taggedRequest("unsupported"))

        val response = interceptor.intercept(chain)

        assertEquals(1, chain.proceedCalls)
        assertEquals(204, response.code)
        assertTrue(logger.messages.any { it.contains("mock generation failed") })
        assertTrue(logger.messages.any { it.contains("Pass-through GET https://example.com/unsupported") })
    }

    @Test
    public fun `registered fake value provider is used by default interceptor`() {
        ApiMirage.registerFakeValueProvider(
            ApiMirageFakeValueProvider { request: ApiMirageFakeValueRequest ->
                if (request.property.declaredName == "name") {
                    ApiMirageFakeValueResult.Provided("Custom Name")
                } else {
                    ApiMirageFakeValueResult.Unhandled
                }
            },
        )
        ApiMirage.install(
            ApiMirageConfig(
                enabled = true,
                seed = 11L,
                diagnostics = ApiMirageDiagnostics.LOGS,
            ),
        )
        val interceptor = ApiMirageInterceptor()
        val chain = FakeChain(request = taggedRequest("user"))

        val response = interceptor.intercept(chain)
        val body = response.body.string()

        assertEquals(0, chain.proceedCalls)
        assertTrue(body.contains("\"name\":\"Custom Name\""))
    }

    @Test
    public fun `registered endpoint override can replace generated payload per endpoint`() {
        ApiMirage.registerEndpointOverride { request ->
            if (request.endpoint.operationName == "user") {
                ApiMirageGenerationResult.Success(UserDto(id = 700L, name = "Override Name"))
            } else {
                null
            }
        }
        ApiMirage.install(
            ApiMirageConfig(
                enabled = true,
                seed = 22L,
                diagnostics = ApiMirageDiagnostics.LOGS,
            ),
        )
        val interceptor = ApiMirageInterceptor()
        val chain = FakeChain(request = taggedRequest("user"))

        val response = interceptor.intercept(chain)
        val body = response.body.string()

        assertEquals(0, chain.proceedCalls)
        assertTrue(body.contains("\"id\":700"))
        assertTrue(body.contains("\"name\":\"Override Name\""))
    }

    private fun taggedRequest(methodName: String): Request {
        val method = SampleService::class.java.methods.single { it.name == methodName }
        val invocation = Invocation.of(method, emptyList<Any>())
        return Request.Builder()
            .url("https://example.com/$methodName")
            .get()
            .tag(Invocation::class.java, invocation)
            .build()
    }

    private interface SampleService {
        @GET("user")
        fun user(): retrofit2.Call<UserDto>

        @com.apimirage.core.annotations.NoAutoMock
        @GET("real-user")
        fun realUser(): retrofit2.Call<UserDto>

        @GET("unsupported")
        fun unsupported(): retrofit2.Call<UnsupportedType>
    }

    data class UserDto(
        val id: Long,
        val name: String,
    )

    class UnsupportedType

    private class RecordingLogger : ApiMirageRetrofitLogger {
        val messages: MutableList<String> = mutableListOf()

        override fun log(config: ApiMirageConfig, message: String) {
            if (config.diagnostics == ApiMirageDiagnostics.LOGS) {
                messages += message
            }
        }
    }

    private class FakeChain(
        private val request: Request,
    ) : Interceptor.Chain {
        var proceedCalls: Int = 0
            private set

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            proceedCalls += 1
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body("".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun call(): Call {
            throw UnsupportedOperationException("Not needed in tests.")
        }

        override fun connectTimeoutMillis(): Int = 10_000

        override fun connection(): Connection? = null

        override fun readTimeoutMillis(): Int = 10_000

        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this

        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this

        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 10_000
    }
}
