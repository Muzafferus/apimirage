package com.apimirage.core

import com.apimirage.core.fake.ApiMirageFakeValueProvider
import com.apimirage.core.fake.ApiMirageFakeValueRequest
import com.apimirage.core.fake.ApiMirageFakeValueResult
import com.apimirage.core.generation.ApiMirageGenerationResult
import com.apimirage.core.generation.ApiMirageRandomSource
import com.apimirage.core.generation.ApiMirageTypeKey
import com.apimirage.core.hooks.ApiMirageEndpointDescriptor
import com.apimirage.core.hooks.ApiMirageEndpointOverride
import com.apimirage.core.hooks.ApiMirageEndpointOverrideRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageCustomizationsTest {
    @After
    public fun tearDown() {
        ApiMirage.resetForTesting()
    }

    @Test
    public fun `register fake value provider adds provider to global extensions`() {
        val provider = ApiMirageFakeValueProvider { _: ApiMirageFakeValueRequest ->
            ApiMirageFakeValueResult.Unhandled
        }

        ApiMirage.registerFakeValueProvider(provider)

        val extensions = ApiMirage.currentExtensions()
        assertEquals(1, extensions.fakeValueProviders.size)
        assertTrue(provider === extensions.fakeValueProviders.first())
        assertFalse(extensions.isEmpty)
    }

    @Test
    public fun `register endpoint override adds override to global extensions`() {
        val override = ApiMirageEndpointOverride { _: ApiMirageEndpointOverrideRequest ->
            ApiMirageGenerationResult.PassThrough("not used")
        }

        ApiMirage.registerEndpointOverride(override)

        val extensions = ApiMirage.currentExtensions()
        assertEquals(1, extensions.endpointOverrides.size)
        assertTrue(override === extensions.endpointOverrides.first())
        assertFalse(extensions.isEmpty)
    }

    @Test
    public fun `clear customizations resets global extension registry`() {
        ApiMirage.registerFakeValueProvider(
            ApiMirageFakeValueProvider { _: ApiMirageFakeValueRequest ->
                ApiMirageFakeValueResult.Unhandled
            },
        )
        ApiMirage.registerEndpointOverride(
            ApiMirageEndpointOverride { _: ApiMirageEndpointOverrideRequest ->
                ApiMirageGenerationResult.Success("value")
            },
        )

        ApiMirage.clearCustomizations()

        assertTrue(ApiMirage.currentExtensions().isEmpty)
    }

    @Test
    public fun `registered override receives endpoint metadata shape`() {
        val request = ApiMirageEndpointOverrideRequest(
            endpoint = ApiMirageEndpointDescriptor(
                adapterName = "retrofit",
                operationName = "getUser",
            ),
            targetType = ApiMirageTypeKey.fromRawClass(String::class),
            random = ApiMirageRandomSource.fromSeed(1L),
        )
        val override = ApiMirageEndpointOverride { incoming ->
            ApiMirageGenerationResult.Success("${incoming.endpoint.adapterName}:${incoming.endpoint.operationName}")
        }

        val result = override.override(request) as ApiMirageGenerationResult.Success

        assertEquals("retrofit:getUser", result.value)
    }
}
