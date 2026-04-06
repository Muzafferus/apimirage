package com.apimirage.retrofit

import java.lang.reflect.Method
import okhttp3.Request
import retrofit2.Invocation

/**
 * Normalized Retrofit invocation metadata extracted from an OkHttp request tag.
 */
public data class RetrofitInvocationContext(
    val request: Request? = null,
    val invocation: Invocation,
    val service: Class<*>,
    val method: Method,
    val arguments: List<Any?>,
    val instance: Any? = null,
)

