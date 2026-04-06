package com.apimirage.core.serialization

import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.core.generation.ApiMirageTypeKey
import com.apimirage.core.generation.ApiMirageValuePath
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Default JSON encoder for generated mock values.
 *
 * Strategy order:
 * 1. Try kotlinx.serialization using the resolved target type.
 * 2. Fall back to reflective JsonElement building for supported v1 shapes.
 * 3. If neither path can handle the value, safely pass through or emit a diagnostic.
 */
public class DefaultApiMirageJsonEncoder(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
        prettyPrint = false
    },
) : ApiMirageJsonEncoder {
    override fun encode(request: ApiMirageJsonEncodingRequest): ApiMirageJsonEncodingResult {
        encodeWithKotlinx(request)?.let { return it }

        return when (
            val fallback = encodeReflectively(
                value = request.value,
                targetType = request.targetType,
                path = request.path,
                diagnostics = request.diagnostics,
                visited = emptySet(),
            )
        ) {
            is ReflectiveEncodingResult.Success -> ApiMirageJsonEncodingResult.Success(
                json = json.encodeToString(JsonElement.serializer(), fallback.element),
                strategy = ApiMirageJsonEncodingStrategy.REFLECTION_FALLBACK,
            )

            is ReflectiveEncodingResult.Failure -> failure(
                diagnostics = request.diagnostics,
                message = fallback.message,
                path = fallback.path,
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun encodeWithKotlinx(
        request: ApiMirageJsonEncodingRequest,
    ): ApiMirageJsonEncodingResult.Success? {
        val serializer = request.targetType.toKSerializerOrNull() ?: return null
        val payload = json.encodeToString(serializer, request.value)
        return ApiMirageJsonEncodingResult.Success(
            json = payload,
            strategy = ApiMirageJsonEncodingStrategy.KOTLINX_SERIALIZATION,
        )
    }

    private fun encodeReflectively(
        value: Any?,
        targetType: ApiMirageTypeKey,
        path: ApiMirageValuePath,
        diagnostics: ApiMirageDiagnostics,
        visited: Set<Int>,
    ): ReflectiveEncodingResult {
        if (value == null) {
            return ReflectiveEncodingResult.Success(JsonNull)
        }

        return when (value) {
            is String -> ReflectiveEncodingResult.Success(JsonPrimitive(value))
            is Number -> ReflectiveEncodingResult.Success(JsonPrimitive(value))
            is Boolean -> ReflectiveEncodingResult.Success(JsonPrimitive(value))
            is Char -> ReflectiveEncodingResult.Success(JsonPrimitive(value.toString()))
            is Enum<*> -> ReflectiveEncodingResult.Success(JsonPrimitive(value.name))
            is Iterable<*> -> encodeIterable(
                value = value,
                targetType = targetType,
                path = path,
                diagnostics = diagnostics,
                visited = visited,
            )

            is Map<*, *> -> encodeMap(
                value = value,
                targetType = targetType,
                path = path,
                diagnostics = diagnostics,
                visited = visited,
            )

            else -> encodeObject(
                value = value,
                targetType = targetType,
                path = path,
                diagnostics = diagnostics,
                visited = visited,
            )
        }
    }

    private fun encodeIterable(
        value: Iterable<*>,
        targetType: ApiMirageTypeKey,
        path: ApiMirageValuePath,
        diagnostics: ApiMirageDiagnostics,
        visited: Set<Int>,
    ): ReflectiveEncodingResult {
        val elementType = targetType.arguments.firstOrNull()
            ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true)

        val elements = mutableListOf<JsonElement>()
        value.forEachIndexed { index, item ->
            when (
                val result = encodeReflectively(
                    value = item,
                    targetType = elementType,
                    path = path.index(index),
                    diagnostics = diagnostics,
                    visited = visited,
                )
            ) {
                is ReflectiveEncodingResult.Success -> elements += result.element
                is ReflectiveEncodingResult.Failure -> return result
            }
        }

        return ReflectiveEncodingResult.Success(JsonArray(elements))
    }

    private fun encodeMap(
        value: Map<*, *>,
        targetType: ApiMirageTypeKey,
        path: ApiMirageValuePath,
        diagnostics: ApiMirageDiagnostics,
        visited: Set<Int>,
    ): ReflectiveEncodingResult {
        val valueType = targetType.arguments.getOrNull(1)
            ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true)

        val elements = linkedMapOf<String, JsonElement>()
        value.forEach { (key, item) ->
            val keyString = key.toJsonObjectKey()
                ?: return ReflectiveEncodingResult.Failure(
                    message = "Unsupported map key ${key?.javaClass?.name} at $path",
                    path = path,
                )

            when (
                val result = encodeReflectively(
                    value = item,
                    targetType = valueType,
                    path = path.child(keyString),
                    diagnostics = diagnostics,
                    visited = visited,
                )
            ) {
                is ReflectiveEncodingResult.Success -> elements[keyString] = result.element
                is ReflectiveEncodingResult.Failure -> return result
            }
        }

        return ReflectiveEncodingResult.Success(JsonObject(elements))
    }

    private fun encodeObject(
        value: Any,
        targetType: ApiMirageTypeKey,
        path: ApiMirageValuePath,
        diagnostics: ApiMirageDiagnostics,
        visited: Set<Int>,
    ): ReflectiveEncodingResult {
        val rawClass = targetType.rawClass ?: value::class
        if (!rawClass.isData) {
            return ReflectiveEncodingResult.Failure(
                message = "Unsupported reflective JSON shape ${rawClass.qualifiedName} at $path",
                path = path,
            )
        }

        val identity = System.identityHashCode(value)
        if (identity in visited) {
            return ReflectiveEncodingResult.Failure(
                message = "Cyclic reference detected while encoding ${rawClass.qualifiedName} at $path",
                path = path,
            )
        }

        val fields = linkedMapOf<String, JsonElement>()
        val typeBindings = rawClass.typeParameters
            .zip(targetType.arguments)
            .associate { (parameter, argument) -> parameter to argument }

        rawClass.memberProperties.forEach { property ->
            @Suppress("UNCHECKED_CAST")
            val typedProperty = property as KProperty1<Any, *>
            typedProperty.isAccessible = true

            val propertyName = property.name
            val propertyValue = typedProperty.get(value)
            val propertyType = propertyValue?.let { ApiMirageTypeKey.fromValue(it) }
                ?: property.returnType.toTypeKeyOrUnknown(typeBindings)

            when (
                val result = encodeReflectively(
                    value = propertyValue,
                    targetType = propertyType,
                    path = path.child(propertyName),
                    diagnostics = diagnostics,
                    visited = visited + identity,
                )
            ) {
                is ReflectiveEncodingResult.Success -> fields[propertyName] = result.element
                is ReflectiveEncodingResult.Failure -> return result
            }
        }

        return ReflectiveEncodingResult.Success(JsonObject(fields))
    }

    private fun Any?.toJsonObjectKey(): String? {
        return when (this) {
            null -> null
            is String -> this
            is Number -> toString()
            is Boolean -> toString()
            is Char -> toString()
            is Enum<*> -> name
            else -> null
        }
    }

    private fun failure(
        diagnostics: ApiMirageDiagnostics,
        message: String,
        path: ApiMirageValuePath,
    ): ApiMirageJsonEncodingResult {
        return if (diagnostics == ApiMirageDiagnostics.LOGS) {
            ApiMirageJsonEncodingResult.Diagnostic(message = message, path = path)
        } else {
            ApiMirageJsonEncodingResult.PassThrough(reason = message)
        }
    }

    private sealed interface ReflectiveEncodingResult {
        data class Success(val element: JsonElement) : ReflectiveEncodingResult

        data class Failure(
            val message: String,
            val path: ApiMirageValuePath,
        ) : ReflectiveEncodingResult
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun ApiMirageTypeKey.toKSerializerOrNull(): KSerializer<Any?>? {
    val type = toKTypeOrNull() ?: return null

    @Suppress("UNCHECKED_CAST")
    return serializerOrNull(type) as KSerializer<Any?>?
}

private fun ApiMirageTypeKey.toKTypeOrNull(): kotlin.reflect.KType? {
    val rawClass = rawClass ?: return null
    val typeArguments = arguments.map { argument ->
        val nestedType = argument.toKTypeOrNull() ?: return null
        KTypeProjection.invariant(nestedType)
    }
    return try {
        rawClass.createType(arguments = typeArguments, nullable = nullable)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun kotlin.reflect.KType.toTypeKeyOrUnknown(
    bindings: Map<KTypeParameter, ApiMirageTypeKey> = emptyMap(),
): ApiMirageTypeKey {
    val classifier = classifier
    return when (classifier) {
        is KClass<*> -> ApiMirageTypeKey.fromRawClass(
            rawClass = classifier,
            nullable = isMarkedNullable,
            arguments = arguments.map { argument ->
                argument.type?.toTypeKeyOrUnknown(bindings)
                    ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true)
            },
        )

        is KTypeParameter -> bindings[classifier]
            ?.let { bound ->
                if (isMarkedNullable && !bound.nullable) {
                    bound.copy(nullable = true)
                } else {
                    bound
                }
            }
            ?: ApiMirageTypeKey.unknown(classifier.name, nullable = isMarkedNullable)

        else -> ApiMirageTypeKey.unknown(toString(), nullable = isMarkedNullable)
    }
}

private fun ApiMirageTypeKey.Companion.fromValue(value: Any): ApiMirageTypeKey {
    val rawClass = value::class
    val arguments = when (value) {
        is List<*> -> listOf(
            value.firstOrNull()?.let { fromValue(it) } ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true),
        )

        is Set<*> -> listOf(
            value.firstOrNull()?.let { fromValue(it) } ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true),
        )

        is Map<*, *> -> listOf(
            value.keys.firstOrNull()?.let { fromValue(it) } ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true),
            value.values.firstOrNull()?.let { fromValue(it) } ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true),
        )

        else -> emptyList()
    }

    return ApiMirageTypeKey.fromRawClass(rawClass, arguments = arguments)
}
