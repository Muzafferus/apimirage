package com.apimirage.core.hooks

import com.apimirage.core.fake.ApiMirageFakeValueProvider
import com.apimirage.core.fake.ApiMirageFakeValueRequest
import com.apimirage.core.fake.ApiMirageFakeValueResult
import com.apimirage.core.generation.ApiMirageGenerationResult
import com.apimirage.core.generation.ApiMirageRandomSource
import com.apimirage.core.generation.ApiMirageTypeKey
import kotlin.String
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageExtensionRegistryTest {
    @Test
    public fun `default registry starts empty`() {
        assertTrue(ApiMirageExtensionRegistry().isEmpty)
    }

    @Test
    public fun `registry reports non-empty when hooks are registered`() {
        val registry = ApiMirageExtensionRegistry(
            fakeValueProviders = listOf(
                ApiMirageFakeValueProvider { _: ApiMirageFakeValueRequest ->
                    ApiMirageFakeValueResult.Unhandled
                },
            ),
            endpointOverrides = listOf(
                ApiMirageEndpointOverride { request ->
                    ApiMirageGenerationResult.Diagnostic(
                        message = request.endpoint.adapterName,
                    )
                },
            ),
        )

        assertFalse(registry.isEmpty)
    }

    @Test
    public fun `endpoint override request carries endpoint and target type`() {
        val request = ApiMirageEndpointOverrideRequest(
            endpoint = ApiMirageEndpointDescriptor(
                adapterName = "retrofit",
                operationName = "getUser",
            ),
            targetType = ApiMirageTypeKey.fromRawClass(String::class),
            random = ApiMirageRandomSource.fromSeed(55L),
        )

        assertEquals("retrofit", request.endpoint.adapterName)
        assertEquals("String", request.targetType.simpleName)
    }
}
