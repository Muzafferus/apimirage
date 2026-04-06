package com.apimirage.core.hooks

import com.apimirage.core.generation.ApiMirageGenerationResult
import com.apimirage.core.generation.ApiMirageRandomSource
import com.apimirage.core.generation.ApiMirageTypeKey
import com.apimirage.core.generation.ApiMirageValuePath

/**
 * Future hook for endpoint-level custom mock behavior.
 */
public fun interface ApiMirageEndpointOverride {
    public fun override(request: ApiMirageEndpointOverrideRequest): ApiMirageGenerationResult?
}

/**
 * Context passed to endpoint-level override hooks.
 */
public data class ApiMirageEndpointOverrideRequest(
    val endpoint: ApiMirageEndpointDescriptor,
    val targetType: ApiMirageTypeKey,
    val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    val random: ApiMirageRandomSource,
)

