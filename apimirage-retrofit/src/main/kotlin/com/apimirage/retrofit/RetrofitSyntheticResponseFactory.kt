package com.apimirage.retrofit

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

internal class RetrofitSyntheticResponseFactory {
    fun create(
        chain: Interceptor.Chain,
        json: String,
    ): Response {
        val request = chain.request()
        val body = json.toResponseBody(JSON_MEDIA_TYPE)
        val now = System.currentTimeMillis()

        return Response.Builder()
            .request(request)
            .protocol(chain.connection()?.protocol() ?: Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .header("Content-Type", JSON_MEDIA_TYPE.toString())
            .sentRequestAtMillis(now)
            .receivedResponseAtMillis(now)
            .build()
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

