package com.apimirage.core.generation

import kotlin.reflect.KClass

/**
 * Adapter-neutral description of a target type to mock.
 */
public data class ApiMirageTypeKey(
    val rawClass: KClass<*>? = null,
    val rawTypeName: String,
    val nullable: Boolean = false,
    val arguments: List<ApiMirageTypeKey> = emptyList(),
) {
    init {
        require(rawTypeName.isNotBlank()) { "rawTypeName must not be blank" }
    }

    public val simpleName: String
        get() = rawClass?.simpleName ?: rawTypeName.substringAfterLast('.')

    public companion object {
        public fun fromRawClass(
            rawClass: KClass<*>,
            nullable: Boolean = false,
            arguments: List<ApiMirageTypeKey> = emptyList(),
        ): ApiMirageTypeKey {
            return ApiMirageTypeKey(
                rawClass = rawClass,
                rawTypeName = rawClass.qualifiedName ?: rawClass.simpleName.orEmpty(),
                nullable = nullable,
                arguments = arguments,
            )
        }

        public fun unknown(
            rawTypeName: String,
            nullable: Boolean = false,
            arguments: List<ApiMirageTypeKey> = emptyList(),
        ): ApiMirageTypeKey {
            return ApiMirageTypeKey(
                rawClass = null,
                rawTypeName = rawTypeName,
                nullable = nullable,
                arguments = arguments,
            )
        }
    }
}

