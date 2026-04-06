package com.apimirage.core

import java.util.concurrent.atomic.AtomicReference

/**
 * Global entry point for configuring ApiMirage inside a consumer app process.
 *
 * The MVP intentionally keeps this surface small:
 * - [install] with no arguments uses build-type defaults
 * - [install] with a Boolean allows a one-liner toggle
 * - [install] with [ApiMirageConfig] exposes deterministic seeds and diagnostics
 */
public object ApiMirage {
    private val configRef: AtomicReference<ApiMirageConfig> =
        AtomicReference(ApiMirageConfig())

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

    internal fun resetForTesting() {
        configRef.set(ApiMirageConfig())
    }
}

