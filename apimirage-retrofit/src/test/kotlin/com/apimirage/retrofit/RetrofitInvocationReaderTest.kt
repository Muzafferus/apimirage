package com.apimirage.retrofit

import java.lang.reflect.Proxy
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Invocation
import retrofit2.http.GET
import retrofit2.http.Path

public class RetrofitInvocationReaderTest {
    private val reader = RetrofitInvocationReader()

    @Test
    public fun `reader extracts invocation metadata from request tag`() {
        val method = SampleService::class.java.getMethod("user", String::class.java)
        val invocation = invocationFor(method, listOf("42"))
        val request = Request.Builder()
            .url("https://example.com/users/42")
            .tag(Invocation::class.java, invocation)
            .build()

        val context = reader.read(request)

        assertNotNull(context)
        assertEquals(SampleService::class.java, context?.service)
        assertEquals("user", context?.method?.name)
        assertEquals(listOf("42"), context?.arguments)
    }

    @Test
    public fun `reader returns null when invocation tag is missing`() {
        val request = Request.Builder()
            .url("https://example.com/users/42")
            .build()

        assertNull(reader.read(request))
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

    private interface SampleService {
        @GET("users/{id}")
        fun user(@Path("id") id: String)
    }
}

