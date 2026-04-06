package com.apimirage.core.generation

import com.apimirage.core.fake.ApiMiragePropertyContext
import com.apimirage.core.hooks.ApiMirageEndpointDescriptor
import com.apimirage.core.hooks.ApiMirageExtensionRegistry

/**
 * Normalized generation input shared across adapters.
 */
public data class ApiMirageGenerationRequest(
    val targetType: ApiMirageTypeKey,
    val random: ApiMirageRandomSource,
    val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    val property: ApiMiragePropertyContext = ApiMiragePropertyContext(),
    val endpoint: ApiMirageEndpointDescriptor? = null,
    val extensions: ApiMirageExtensionRegistry = ApiMirageExtensionRegistry(),
)

