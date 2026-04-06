package com.apimirage.core

internal object VariantApiMirageDefaults : ApiMirageVariantDefaults {
    private val defaults = ApiMirageDefaultPolicy.forDebuggableBuild(isDebuggableBuild = false)

    override val enabledByDefault: Boolean = defaults.enabled
    override val diagnosticsByDefault: ApiMirageDiagnostics = defaults.diagnostics
}
