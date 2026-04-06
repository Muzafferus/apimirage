package com.apimirage.core.fake

import com.apimirage.core.generation.ApiMirageRandomSource
import com.apimirage.core.generation.ApiMirageTypeKey
import com.apimirage.core.generation.ApiMirageValuePath

/**
 * Context passed to field-level fake value providers.
 */
public data class ApiMirageFakeValueRequest(
    val targetType: ApiMirageTypeKey,
    val property: ApiMiragePropertyContext = ApiMiragePropertyContext(),
    val path: ApiMirageValuePath = ApiMirageValuePath.root(),
    val random: ApiMirageRandomSource,
)

