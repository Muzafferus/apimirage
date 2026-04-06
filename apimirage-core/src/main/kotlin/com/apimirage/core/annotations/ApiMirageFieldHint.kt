package com.apimirage.core.annotations

import com.apimirage.core.fake.ApiMirageValueHint

/**
 * Overrides the inferred fake value hint for a specific field or constructor parameter.
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ApiMirageFieldHint(
    val value: ApiMirageValueHint,
)

