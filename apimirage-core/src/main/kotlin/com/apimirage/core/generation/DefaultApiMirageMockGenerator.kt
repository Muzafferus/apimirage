package com.apimirage.core.generation

import com.apimirage.core.fake.ApiMirageFakeValueRequest
import com.apimirage.core.fake.ApiMirageFakeValueResult
import com.apimirage.core.fake.ApiMiragePropertyContext
import com.apimirage.core.fake.DefaultApiMirageFakeValueProvider
import com.apimirage.core.hooks.ApiMirageEndpointOverrideRequest
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Default recursive generator for supported v1 ApiMirage model shapes.
 */
public class DefaultApiMirageMockGenerator(
    private val defaultFakeValueProvider: DefaultApiMirageFakeValueProvider =
        DefaultApiMirageFakeValueProvider(),
    private val collectionSize: Int = 3,
    private val mapSize: Int = 2,
    private val maxDepth: Int = 8,
) : ApiMirageMockGenerator {
    override fun generate(request: ApiMirageGenerationRequest): ApiMirageGenerationResult {
        request.endpoint?.let { endpoint ->
            request.extensions.endpointOverrides.forEach { override ->
                override.override(
                    ApiMirageEndpointOverrideRequest(
                        endpoint = endpoint,
                        targetType = request.targetType,
                        path = request.path,
                        random = request.random,
                    ),
                )?.let { return it }
            }
        }

        return generateValue(request)
    }

    private fun generateValue(request: ApiMirageGenerationRequest): ApiMirageGenerationResult {
        if (request.path.segments.size > maxDepth) {
            return if (request.targetType.nullable) {
                ApiMirageGenerationResult.Success(null)
            } else {
                ApiMirageGenerationResult.PassThrough(
                    "Exceeded max generation depth for ${request.targetType.rawTypeName} at ${request.path}",
                )
            }
        }

        val rawClass = request.targetType.rawClass
            ?: return unsupportedResult(request)

        provideLeafValue(request)?.let { return it }

        if (rawClass.java.isEnum) {
            val values = rawClass.java.enumConstants.orEmpty()
            if (values.isNotEmpty()) {
                return ApiMirageGenerationResult.Success(values[request.random.nextInt(values.size)])
            }
        }

        if (Collection::class.java.isAssignableFrom(rawClass.java)) {
            return generateCollection(request)
        }

        if (Map::class.java.isAssignableFrom(rawClass.java)) {
            return generateMap(request)
        }

        if (rawClass.isData) {
            return generateDataClass(request, rawClass)
        }

        return unsupportedResult(request)
    }

    private fun provideLeafValue(
        request: ApiMirageGenerationRequest,
    ): ApiMirageGenerationResult? {
        val fakeRequest = ApiMirageFakeValueRequest(
            targetType = request.targetType,
            property = request.property,
            path = request.path,
            random = request.random,
        )

        val providers = request.extensions.fakeValueProviders + defaultFakeValueProvider
        providers.forEach { provider ->
            when (val result = provider.provide(fakeRequest)) {
                is ApiMirageFakeValueResult.Provided -> {
                    if (result.value == null && !request.targetType.nullable) {
                        return ApiMirageGenerationResult.Diagnostic(
                            message = "Fake value provider returned null for non-null ${request.targetType.rawTypeName}",
                            path = request.path,
                        )
                    }
                    return ApiMirageGenerationResult.Success(result.value)
                }

                ApiMirageFakeValueResult.Unhandled -> Unit
            }
        }

        return null
    }

    private fun generateCollection(
        request: ApiMirageGenerationRequest,
    ): ApiMirageGenerationResult {
        val elementType = request.targetType.arguments.firstOrNull()
            ?: ApiMirageTypeKey.fromRawClass(String::class)

        val items = buildList(collectionSize) {
            repeat(collectionSize) { index ->
                when (
                    val itemResult = generateValue(
                        request.copy(
                            targetType = elementType,
                            path = request.path.index(index),
                            property = ApiMiragePropertyContext(),
                            random = request.random.fork("item-$index"),
                        ),
                    )
                ) {
                    is ApiMirageGenerationResult.Success -> add(itemResult.value)
                    else -> return itemResult
                }
            }
        }

        val collection = when {
            Set::class.java.isAssignableFrom(request.targetType.rawClass!!.java) -> linkedSetOf<Any?>().apply {
                addAll(items)
            }

            else -> items
        }

        return ApiMirageGenerationResult.Success(collection)
    }

    private fun generateMap(
        request: ApiMirageGenerationRequest,
    ): ApiMirageGenerationResult {
        val keyType = request.targetType.arguments.getOrNull(0)
            ?: ApiMirageTypeKey.fromRawClass(String::class)
        val valueType = request.targetType.arguments.getOrNull(1)
            ?: ApiMirageTypeKey.fromRawClass(String::class)

        val entries = linkedMapOf<Any, Any?>()
        repeat(mapSize) { index ->
            val keyResult = generateValue(
                request.copy(
                    targetType = keyType,
                    path = request.path.child("key$index"),
                    property = ApiMiragePropertyContext.from("key$index"),
                    random = request.random.fork("key-$index"),
                ),
            )
            val key = when (keyResult) {
                is ApiMirageGenerationResult.Success -> keyResult.value
                else -> return keyResult
            } ?: return ApiMirageGenerationResult.PassThrough(
                "Map keys cannot be null for ${request.targetType.rawTypeName}",
            )

            val valueResult = generateValue(
                request.copy(
                    targetType = valueType,
                    path = request.path.child(key.toString()),
                    property = ApiMiragePropertyContext.from(key.toString()),
                    random = request.random.fork("value-$index"),
                ),
            )
            val value = when (valueResult) {
                is ApiMirageGenerationResult.Success -> valueResult.value
                else -> return valueResult
            }

            entries[key] = value
        }

        return ApiMirageGenerationResult.Success(entries)
    }

    private fun generateDataClass(
        request: ApiMirageGenerationRequest,
        rawClass: KClass<*>,
    ): ApiMirageGenerationResult {
        val constructor = rawClass.primaryConstructor
            ?: return unsupportedResult(request)

        val typeBindings = rawClass.typeParameters
            .zip(request.targetType.arguments)
            .associate { (parameter, argument) -> parameter to argument }

        val propertyAnnotations = rawClass.memberProperties
            .associateBy({ it.name }, { it.annotations })

        val arguments = linkedMapOf<KParameter, Any?>()
        constructor.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .forEachIndexed { index, parameter ->
                val parameterName = parameter.name ?: "arg$index"
                val annotations = parameter.annotations + propertyAnnotations[parameterName].orEmpty()
                val parameterType = resolveTypeKey(parameter.type, typeBindings)

                val parameterResult = generateValue(
                    request.copy(
                        targetType = parameterType,
                        path = request.path.child(parameterName),
                        property = ApiMiragePropertyContext.from(
                            declaredName = parameterName,
                            annotations = annotations,
                        ),
                        random = request.random.fork(parameterName),
                    ),
                )

                when (parameterResult) {
                    is ApiMirageGenerationResult.Success -> arguments[parameter] = parameterResult.value
                    is ApiMirageGenerationResult.PassThrough -> {
                        if (parameter.isOptional) {
                            return@forEachIndexed
                        }
                        if (parameter.type.isMarkedNullable) {
                            arguments[parameter] = null
                        } else {
                            return parameterResult
                        }
                    }

                    is ApiMirageGenerationResult.Diagnostic -> return parameterResult
                }
            }

        return try {
            ApiMirageGenerationResult.Success(constructor.callBy(arguments))
        } catch (throwable: Throwable) {
            ApiMirageGenerationResult.Diagnostic(
                message = "Failed to instantiate ${rawClass.qualifiedName}: ${throwable.message}",
                path = request.path,
            )
        }
    }

    private fun resolveTypeKey(
        type: KType,
        bindings: Map<KTypeParameter, ApiMirageTypeKey>,
    ): ApiMirageTypeKey {
        val classifier = type.classifier

        return when (classifier) {
            is KClass<*> -> ApiMirageTypeKey(
                rawClass = classifier,
                rawTypeName = classifier.qualifiedName ?: classifier.simpleName.orEmpty(),
                nullable = type.isMarkedNullable,
                arguments = type.arguments.map { argument ->
                    argument.type?.let { resolveTypeKey(it, bindings) }
                        ?: ApiMirageTypeKey.unknown("kotlin.Any", nullable = true)
                },
            )

            is KTypeParameter -> {
                val boundType = bindings[classifier]
                    ?: ApiMirageTypeKey.unknown(classifier.name, nullable = type.isMarkedNullable)

                if (type.isMarkedNullable && !boundType.nullable) {
                    boundType.copy(nullable = true)
                } else {
                    boundType
                }
            }

            else -> ApiMirageTypeKey.unknown(type.toString(), nullable = type.isMarkedNullable)
        }
    }

    private fun unsupportedResult(
        request: ApiMirageGenerationRequest,
    ): ApiMirageGenerationResult {
        return if (request.targetType.nullable) {
            ApiMirageGenerationResult.Success(null)
        } else {
            ApiMirageGenerationResult.PassThrough(
                "Unsupported mock type ${request.targetType.rawTypeName} at ${request.path}",
            )
        }
    }
}
