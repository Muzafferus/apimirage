package com.apimirage.retrofit

import com.apimirage.core.generation.ApiMirageTypeKey
import java.lang.reflect.Type

/**
 * Detailed Retrofit response body resolution result used by the interceptor layer.
 */
public sealed interface RetrofitResponseTypeResolution {
    public data class Success(
        val invocation: RetrofitInvocationContext,
        val resolvedType: ResolvedType,
    ) : RetrofitResponseTypeResolution

    public data class Unsupported(
        val reason: String,
        val invocation: RetrofitInvocationContext? = null,
    ) : RetrofitResponseTypeResolution

    public data class ResolvedType(
        val declaration: RetrofitResponseDeclaration,
        val bodyJavaType: Type,
        val bodyType: ApiMirageTypeKey,
    )
}

public enum class RetrofitResponseDeclaration {
    CALL,
    RESPONSE,
    BODY,
    SUSPEND_BODY,
    SUSPEND_RESPONSE,
}

