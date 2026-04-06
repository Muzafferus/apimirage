package com.apimirage.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageConfigExtensionsTest {
    @Test
    public fun `newRandomSource uses config seed for deterministic generation`() {
        val first = ApiMirageConfig(seed = 99L).newRandomSource()
        val second = ApiMirageConfig(seed = 99L).newRandomSource()

        assertTrue(first.isDeterministic)
        assertEquals(first.nextInt(1_000), second.nextInt(1_000))
        assertEquals(first.nextAlphaNumeric(12), second.nextAlphaNumeric(12))
    }
}

