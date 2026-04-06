package com.apimirage.core.serialization

/**
 * Encodes generated mock objects into JSON for adapter integrations.
 */
public fun interface ApiMirageJsonEncoder {
    public fun encode(request: ApiMirageJsonEncodingRequest): ApiMirageJsonEncodingResult
}

