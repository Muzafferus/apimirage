package com.apimirage.core

internal data class ApiMirageDefaultSettings(
    val enabled: Boolean,
    val diagnostics: ApiMirageDiagnostics,
)

internal object ApiMirageDefaultPolicy {
    fun forDebuggableBuild(isDebuggableBuild: Boolean): ApiMirageDefaultSettings {
        return if (isDebuggableBuild) {
            ApiMirageDefaultSettings(
                enabled = true,
                diagnostics = ApiMirageDiagnostics.LOGS,
            )
        } else {
            ApiMirageDefaultSettings(
                enabled = false,
                diagnostics = ApiMirageDiagnostics.NONE,
            )
        }
    }
}

