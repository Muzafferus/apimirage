package com.apimirage.core.fake

import com.apimirage.core.annotations.ApiMirageFieldHint

/**
 * Property-level metadata used when picking fake values.
 */
public data class ApiMiragePropertyContext(
    val declaredName: String? = null,
    val annotations: List<Annotation> = emptyList(),
    val explicitHint: ApiMirageValueHint? = null,
) {
    public val hints: Set<ApiMirageValueHint>
        get() = buildSet {
            explicitHint?.let(::add)
            annotations
                .filterIsInstance<ApiMirageFieldHint>()
                .firstOrNull()
                ?.value
                ?.let(::add)
            addAll(ApiMirageValueHintResolver.inferFromName(declaredName))
        }

    public companion object {
        public fun from(
            declaredName: String?,
            annotations: List<Annotation> = emptyList(),
        ): ApiMiragePropertyContext {
            return ApiMiragePropertyContext(
                declaredName = declaredName,
                annotations = annotations,
            )
        }
    }
}

