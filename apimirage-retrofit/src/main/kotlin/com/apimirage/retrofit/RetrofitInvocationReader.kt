package com.apimirage.retrofit

import okhttp3.Request
import retrofit2.Invocation

/**
 * Reads Retrofit invocation metadata that Retrofit stores on OkHttp requests.
 */
public class RetrofitInvocationReader {
    public fun read(request: Request): RetrofitInvocationContext? {
        val invocation = request.tag(Invocation::class.java) ?: return null
        return read(invocation = invocation, request = request)
    }

    public fun read(
        invocation: Invocation,
        request: Request? = null,
    ): RetrofitInvocationContext {
        return RetrofitInvocationContext(
            request = request,
            invocation = invocation,
            service = invocation.service(),
            method = invocation.method(),
            arguments = invocation.arguments().toList(),
            instance = invocation.instance(),
        )
    }
}

