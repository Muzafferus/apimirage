package com.apimirage.core.fake

/**
 * Result returned by a fake value provider.
 */
public sealed interface ApiMirageFakeValueResult {
    public data class Provided(val value: Any?) : ApiMirageFakeValueResult

    public data object Unhandled : ApiMirageFakeValueResult
}

