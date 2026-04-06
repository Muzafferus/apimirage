package com.apimirage.retrofit

import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

public class ApiMirageRetrofitIntegrationTest {
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    @Test
    public fun `retrofit parses synthetic HTTP 200 responses end to end`() {
        val upstream = RecordingUpstreamInterceptor(
            responsesByPath = mapOf(
                "user" to """{"id":999,"name":"Upstream User","email":"upstream@example.com"}""",
                "users" to """[{"id":999,"name":"Upstream User","email":"upstream@example.com"}]""",
                "wrapped-user" to """{"success":false,"message":"upstream","data":{"id":999,"name":"Upstream User","email":"upstream@example.com"}}""",
            ),
        )
        val service = createService(
            config = ApiMirageConfig(
                enabled = true,
                seed = 20260407L,
                diagnostics = ApiMirageDiagnostics.NONE,
            ),
            upstream = upstream,
        )

        val userResponse = service.user().execute()
        val usersResponse = service.users().execute()
        val wrappedResponse = service.wrappedUser().execute()

        assertEquals(0, upstream.calls)
        assertEquals(200, userResponse.code())
        assertEquals("application/json; charset=utf-8", userResponse.raw().header("Content-Type"))
        assertNotNull(userResponse.body())
        assertTrue(userResponse.body()!!.name.isNotBlank())

        assertEquals(200, usersResponse.code())
        assertTrue(usersResponse.body()!!.size >= 3)
        assertTrue(usersResponse.body()!!.all { it.name.isNotBlank() })

        assertEquals(200, wrappedResponse.code())
        assertNotNull(wrappedResponse.body())
        assertTrue(wrappedResponse.body()!!.message.isNotBlank())
        assertTrue(wrappedResponse.body()!!.data.name.isNotBlank())
    }

    @Test
    public fun `same seed produces the same parsed payload across repeated retrofit calls`() {
        val upstream = RecordingUpstreamInterceptor(
            responsesByPath = mapOf(
                "user" to """{"id":1,"name":"Unexpected","email":"unexpected@example.com"}""",
            ),
        )
        val service = createService(
            config = ApiMirageConfig(
                enabled = true,
                seed = 55L,
                diagnostics = ApiMirageDiagnostics.NONE,
            ),
            upstream = upstream,
        )

        val first = service.user().execute().body()
        val second = service.user().execute().body()

        assertEquals(0, upstream.calls)
        assertEquals(first, second)
    }

    @Test
    public fun `disabled interceptor passes through upstream JSON for normal retrofit parsing`() {
        val upstreamUser = UserDto(
            id = 501L,
            name = "Real Network User",
            email = "real@example.com",
        )
        val upstream = RecordingUpstreamInterceptor(
            responsesByPath = mapOf(
                "user" to json.encodeToString(UserDto.serializer(), upstreamUser),
            ),
        )
        val service = createService(
            config = ApiMirageConfig(
                enabled = false,
                diagnostics = ApiMirageDiagnostics.NONE,
            ),
            upstream = upstream,
        )

        val response = service.user().execute()

        assertEquals(1, upstream.calls)
        assertEquals(200, response.code())
        assertEquals(upstreamUser, response.body())
    }

    private fun createService(
        config: ApiMirageConfig,
        upstream: RecordingUpstreamInterceptor,
    ): SampleService {
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiMirageInterceptor(config))
            .addInterceptor(upstream)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(SampleService::class.java)
    }

    public interface SampleService {
        @GET("user")
        fun user(): Call<UserDto>

        @GET("users")
        fun users(): Call<List<UserDto>>

        @GET("wrapped-user")
        fun wrappedUser(): Call<BaseResponse<UserDto>>
    }

    @Serializable
    public data class UserDto(
        val id: Long,
        val name: String,
        val email: String,
    )

    @Serializable
    public data class BaseResponse<T>(
        val success: Boolean,
        val message: String,
        val data: T,
    )

    private class RecordingUpstreamInterceptor(
        private val responsesByPath: Map<String, String>,
    ) : Interceptor {
        var calls: Int = 0
            private set

        override fun intercept(chain: Interceptor.Chain): Response {
            calls += 1
            val path = chain.request().url.encodedPath.trimStart('/')
            val payload = responsesByPath[path]
                ?: error("Unexpected upstream request for $path")

            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", JSON_MEDIA_TYPE.toString())
                .body(payload.toResponseBody(JSON_MEDIA_TYPE))
                .sentRequestAtMillis(10L)
                .receivedResponseAtMillis(10L)
                .build()
        }
    }

    private companion object {
        private const val BASE_URL: String = "https://example.com/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
