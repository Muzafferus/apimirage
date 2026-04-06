package com.apimirage.core.fake

/**
 * Supplies field-level fake values for known property names or annotations.
 */
public fun interface ApiMirageFakeValueProvider {
    public fun provide(request: ApiMirageFakeValueRequest): ApiMirageFakeValueResult
}

