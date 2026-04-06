package com.apimirage.core

internal interface ApiMirageVariantDefaults {
    public val enabledByDefault: Boolean
    public val diagnosticsByDefault: ApiMirageDiagnostics

    public companion object : ApiMirageVariantDefaults by VariantApiMirageDefaults
}

