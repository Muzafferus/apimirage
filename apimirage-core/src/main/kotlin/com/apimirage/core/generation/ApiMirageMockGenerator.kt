package com.apimirage.core.generation

/**
 * Generates values for normalized target types.
 */
public fun interface ApiMirageMockGenerator {
    public fun generate(request: ApiMirageGenerationRequest): ApiMirageGenerationResult
}

