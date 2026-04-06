package com.apimirage.core

import com.apimirage.core.generation.ApiMirageRandomSource

/**
 * Creates a new random source from the current configuration.
 *
 * When [seed] is provided, generated values are deterministic across runs.
 */
public fun ApiMirageConfig.newRandomSource(): ApiMirageRandomSource {
    return ApiMirageRandomSource.fromSeed(seed)
}

