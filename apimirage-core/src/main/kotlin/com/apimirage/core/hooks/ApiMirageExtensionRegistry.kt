package com.apimirage.core.hooks

import com.apimirage.core.fake.ApiMirageFakeValueProvider

/**
 * Registry for future customization hooks without expanding the main public API.
 */
public data class ApiMirageExtensionRegistry(
    val fakeValueProviders: List<ApiMirageFakeValueProvider> = emptyList(),
    val endpointOverrides: List<ApiMirageEndpointOverride> = emptyList(),
) {
    public val isEmpty: Boolean
        get() = fakeValueProviders.isEmpty() && endpointOverrides.isEmpty()

    public fun withFakeValueProvider(
        provider: ApiMirageFakeValueProvider,
    ): ApiMirageExtensionRegistry {
        return copy(fakeValueProviders = fakeValueProviders + provider)
    }

    public fun withEndpointOverride(
        override: ApiMirageEndpointOverride,
    ): ApiMirageExtensionRegistry {
        return copy(endpointOverrides = endpointOverrides + override)
    }
}
