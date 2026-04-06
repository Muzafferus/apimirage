package com.apimirage.core.generation

/**
 * Outcome of attempting to generate a mock value for a type.
 */
public sealed interface ApiMirageGenerationResult {
    public data class Success(val value: Any?) : ApiMirageGenerationResult

    public data class PassThrough(val reason: String) : ApiMirageGenerationResult

    public data class Diagnostic(
        val message: String,
        val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    ) : ApiMirageGenerationResult
}

