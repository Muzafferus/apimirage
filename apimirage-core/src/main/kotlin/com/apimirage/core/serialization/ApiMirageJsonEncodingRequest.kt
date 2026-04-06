package com.apimirage.core.serialization

import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.core.generation.ApiMirageTypeKey
import com.apimirage.core.generation.ApiMirageValuePath

/**
 * Normalized encoding input after a mock value has been generated.
 */
public data class ApiMirageJsonEncodingRequest(
    val value: Any?,
    val targetType: ApiMirageTypeKey,
    val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    val diagnostics: ApiMirageDiagnostics = ApiMirageDiagnostics.NONE,
)

