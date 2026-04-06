package com.apimirage.core.serialization

import com.apimirage.core.generation.ApiMirageValuePath

/**
 * Outcome of attempting to encode a mock object into JSON.
 */
public sealed interface ApiMirageJsonEncodingResult {
    public data class Success(
        val json: String,
        val strategy: ApiMirageJsonEncodingStrategy,
    ) : ApiMirageJsonEncodingResult

    public data class PassThrough(val reason: String) : ApiMirageJsonEncodingResult

    public data class Diagnostic(
        val message: String,
        val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    ) : ApiMirageJsonEncodingResult
}

public enum class ApiMirageJsonEncodingStrategy {
    KOTLINX_SERIALIZATION,
    REFLECTION_FALLBACK,
}

