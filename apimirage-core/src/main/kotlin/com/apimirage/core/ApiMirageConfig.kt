package com.apimirage.core

/**
 * Tiny configuration object for callers that need more than a simple on/off switch.
 *
 * Defaults are build-type aware:
 * - debug variants: mocking enabled and diagnostic logging on
 * - release variants: mocking disabled and diagnostics off
 */
public data class ApiMirageConfig(
    val enabled: Boolean = ApiMirageVariantDefaults.enabledByDefault,
    val seed: Long? = null,
    val diagnostics: ApiMirageDiagnostics = ApiMirageVariantDefaults.diagnosticsByDefault,
)

/**
 * Debug-focused diagnostics level for ApiMirage internals and future adapters.
 */
public enum class ApiMirageDiagnostics {
    NONE,
    LOGS,
}

