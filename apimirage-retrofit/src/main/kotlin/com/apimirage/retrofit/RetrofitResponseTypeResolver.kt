package com.apimirage.retrofit

import com.apimirage.core.generation.ApiMirageTypeKey
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import retrofit2.Call
import retrofit2.Invocation
import retrofit2.Response

/**
 * Resolves the actual Retrofit response body type that ApiMirage should mock.
 */
public class RetrofitResponseTypeResolver(
    private val invocationReader: RetrofitInvocationReader = RetrofitInvocationReader(),
) {
    public fun resolve(request: okhttp3.Request): RetrofitResponseTypeResolution {
        val invocation = invocationReader.read(request)
            ?: return RetrofitResponseTypeResolution.Unsupported(
                reason = "Retrofit Invocation tag was not present on the request",
            )
        return resolve(invocation)
    }

    public fun resolve(invocation: Invocation): RetrofitResponseTypeResolution {
        return resolve(invocationReader.read(invocation))
    }

    public fun resolve(invocation: RetrofitInvocationContext): RetrofitResponseTypeResolution {
        val bodyResolution = resolveMethodBodyType(invocation.method)
            ?: return RetrofitResponseTypeResolution.Unsupported(
                reason = "Unable to resolve Retrofit response type for ${invocation.method}",
                invocation = invocation,
            )

        return RetrofitResponseTypeResolution.Success(
            invocation = invocation,
            resolvedType = RetrofitResponseTypeResolution.ResolvedType(
                declaration = bodyResolution.declaration,
                bodyJavaType = bodyResolution.bodyJavaType,
                bodyType = typeKeyFrom(bodyResolution.bodyJavaType),
            ),
        )
    }

    private fun resolveMethodBodyType(method: java.lang.reflect.Method): UnwrappedBodyType? {
        return if (method.isSuspend()) {
            resolveSuspendBodyType(method)
        } else {
            resolveDirectBodyType(method.genericReturnType)
        }
    }

    private fun resolveDirectBodyType(returnType: Type): UnwrappedBodyType? {
        val rawClass = rawClassOf(returnType) ?: return null
        return when (rawClass) {
            Call::class.java -> parameterizedArgument(returnType, 0)?.let {
                UnwrappedBodyType(RetrofitResponseDeclaration.CALL, unwrapWildcard(it))
            }

            Response::class.java -> parameterizedArgument(returnType, 0)?.let {
                UnwrappedBodyType(RetrofitResponseDeclaration.RESPONSE, unwrapWildcard(it))
            }

            else -> UnwrappedBodyType(RetrofitResponseDeclaration.BODY, unwrapWildcard(returnType))
        }
    }

    private fun resolveSuspendBodyType(method: java.lang.reflect.Method): UnwrappedBodyType? {
        val continuationType = method.genericParameterTypes.lastOrNull() as? ParameterizedType
            ?: return null
        val continuationRawType = rawClassOf(continuationType.rawType) ?: return null
        if (continuationRawType != Continuation::class.java) {
            return null
        }

        val continuationBodyType = continuationType.actualTypeArguments.singleOrNull()
            ?.let(::unwrapWildcard)
            ?: return null

        val rawBodyClass = rawClassOf(continuationBodyType)
        return if (rawBodyClass == Response::class.java) {
            parameterizedArgument(continuationBodyType, 0)?.let {
                UnwrappedBodyType(RetrofitResponseDeclaration.SUSPEND_RESPONSE, unwrapWildcard(it))
            }
        } else {
            UnwrappedBodyType(
                declaration = RetrofitResponseDeclaration.SUSPEND_BODY,
                bodyJavaType = continuationBodyType,
            )
        }
    }

    private fun parameterizedArgument(type: Type, index: Int): Type? {
        val parameterizedType = type as? ParameterizedType ?: return null
        return parameterizedType.actualTypeArguments.getOrNull(index)
    }

    private fun typeKeyFrom(type: Type): ApiMirageTypeKey {
        val unwrappedType = unwrapWildcard(type)
        return when (unwrappedType) {
            is Class<*> -> {
                if (unwrappedType.isArray) {
                    val componentType = unwrappedType.componentType ?: Any::class.java
                    ApiMirageTypeKey.fromRawClass(
                        Array<Any>::class,
                        arguments = listOf(typeKeyFrom(componentType)),
                    )
                } else {
                    ApiMirageTypeKey.fromRawClass(unwrappedType.kotlin)
                }
            }

            is ParameterizedType -> {
                val rawClass = rawClassOf(unwrappedType.rawType)
                if (rawClass != null) {
                    ApiMirageTypeKey(
                        rawClass = rawClass.kotlin,
                        rawTypeName = rawClass.kotlin.qualifiedName ?: rawClass.typeName,
                        arguments = unwrappedType.actualTypeArguments.map(::typeKeyFrom),
                    )
                } else {
                    ApiMirageTypeKey.unknown(
                        rawTypeName = unwrappedType.typeName,
                        arguments = unwrappedType.actualTypeArguments.map(::typeKeyFrom),
                    )
                }
            }

            is GenericArrayType -> ApiMirageTypeKey.fromRawClass(
                Array<Any>::class,
                arguments = listOf(typeKeyFrom(unwrappedType.genericComponentType)),
            )

            is TypeVariable<*> -> {
                val firstBound = unwrappedType.bounds.firstOrNull()
                if (firstBound != null) {
                    typeKeyFrom(firstBound)
                } else {
                    ApiMirageTypeKey.unknown(unwrappedType.name)
                }
            }

            else -> ApiMirageTypeKey.unknown(unwrappedType.typeName)
        }
    }

    private fun unwrapWildcard(type: Type): Type {
        return when (type) {
            is WildcardType -> type.lowerBounds.firstOrNull()
                ?: type.upperBounds.firstOrNull { it != Any::class.java }
                ?: type.upperBounds.firstOrNull()
                ?: Any::class.java

            else -> type
        }
    }

    private fun rawClassOf(type: Type): Class<*>? {
        val unwrappedType = unwrapWildcard(type)
        return when (unwrappedType) {
            is Class<*> -> unwrappedType
            is ParameterizedType -> rawClassOf(unwrappedType.rawType)
            is GenericArrayType -> Array<Any>::class.java
            is TypeVariable<*> -> unwrappedType.bounds.firstOrNull()?.let(::rawClassOf)
            else -> null
        }
    }

    private fun java.lang.reflect.Method.isSuspend(): Boolean {
        return parameterTypes.lastOrNull() == Continuation::class.java
    }

    private data class UnwrappedBodyType(
        val declaration: RetrofitResponseDeclaration,
        val bodyJavaType: Type,
    )
}
