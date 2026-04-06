package com.apimirage.core.fake

import com.apimirage.core.generation.ApiMirageRandomSource
import com.apimirage.core.generation.ApiMirageTypeKey
import org.junit.Assert.assertTrue
import org.junit.Test

public class DefaultApiMirageFakeValueProviderTest {
    private val provider = DefaultApiMirageFakeValueProvider()

    @Test
    public fun `provider emits semantic values for supported string hints`() {
        assertTrue(assertStringValue("email").contains("@example.com"))
        assertTrue(assertStringValue("phone").startsWith("+1-"))
        assertTrue(assertStringValue("url").startsWith("https://"))
        assertTrue(assertStringValue("createdAt").contains("T"))
        assertTrue(assertStringValue("updatedAt").contains("T"))
    }

    @Test
    public fun `provider emits id shaped integers when field looks like id`() {
        val request = ApiMirageFakeValueRequest(
            targetType = ApiMirageTypeKey.fromRawClass(Int::class),
            property = ApiMiragePropertyContext.from("userId"),
            random = ApiMirageRandomSource.fromSeed(12L),
        )

        val result = provider.provide(request) as ApiMirageFakeValueResult.Provided
        assertTrue((result.value as Int) >= 1_000)
    }

    private fun assertStringValue(fieldName: String): String {
        val request = ApiMirageFakeValueRequest(
            targetType = ApiMirageTypeKey.fromRawClass(String::class),
            property = ApiMiragePropertyContext.from(fieldName),
            random = ApiMirageRandomSource.fromSeed(99L),
        )

        val result = provider.provide(request) as ApiMirageFakeValueResult.Provided
        return result.value as String
    }
}
