package com.apimirage.core

import com.apimirage.core.fake.ApiMirageFakeValueProvider
import com.apimirage.core.hooks.ApiMirageEndpointOverride
import com.apimirage.core.hooks.ApiMirageExtensionRegistry
import java.util.concurrent.atomic.AtomicReference

/**
 * Global entry point for configuring ApiMirage inside a consumer app process.
 *
 * The MVP intentionally keeps this surface small:
 * - [install] with no arguments uses build-type defaults
 * - [install] with a Boolean allows a one-liner toggle
 * - [install] with [ApiMirageConfig] exposes deterministic seeds and diagnostics
 * - optional hook registration stays additive for future endpoint-level customization
 */
public object ApiMirage {
    private val configRef: AtomicReference<ApiMirageConfig> =
        AtomicReference(ApiMirageConfig())
    private val extensionsRef: AtomicReference<ApiMirageExtensionRegistry> =
        AtomicReference(ApiMirageExtensionRegistry())

    @JvmStatic
    public fun install() {
        install(ApiMirageConfig())
    }

    @JvmStatic
    public fun install(enabled: Boolean) {
        install(ApiMirageConfig(enabled = enabled))
    }

    @JvmStatic
    public fun install(config: ApiMirageConfig) {
        configRef.set(config)
    }

    @JvmStatic
    public fun currentConfig(): ApiMirageConfig = configRef.get()

    @JvmStatic
    public fun isEnabled(): Boolean = currentConfig().enabled

    @JvmStatic
    public fun currentExtensions(): ApiMirageExtensionRegistry = extensionsRef.get()

    @JvmStatic
    public fun registerFakeValueProvider(provider: ApiMirageFakeValueProvider) {
        updateExtensions { registry -> registry.withFakeValueProvider(provider) }
    }

    @JvmStatic
    public fun registerEndpointOverride(override: ApiMirageEndpointOverride) {
        updateExtensions { registry -> registry.withEndpointOverride(override) }
    }

    @JvmStatic
    public fun clearCustomizations() {
        extensionsRef.set(ApiMirageExtensionRegistry())
    }

    private fun updateExtensions(
        transform: (ApiMirageExtensionRegistry) -> ApiMirageExtensionRegistry,
    ) {
        while (true) {
            val current = extensionsRef.get()
            val updated = transform(current)
            if (extensionsRef.compareAndSet(current, updated)) {
                return
            }
        }
    }

    internal fun resetForTesting() {
        configRef.set(ApiMirageConfig())
        clearCustomizations()
    }
}
