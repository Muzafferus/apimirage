package com.apimirage.core.hooks

/**
 * Adapter-agnostic endpoint metadata for diagnostics and future overrides.
 */
public data class ApiMirageEndpointDescriptor(
    val adapterName: String = "unknown",
    val serviceName: String? = null,
    val operationName: String? = null,
    val httpMethod: String? = null,
    val path: String? = null,
    val arguments: Map<String, Any?> = emptyMap(),
)

