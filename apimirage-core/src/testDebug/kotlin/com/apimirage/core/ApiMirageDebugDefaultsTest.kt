package com.apimirage.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageDebugDefaultsTest {
    @Test
    public fun `debug defaults enable mocking and logs`() {
        val config = ApiMirageConfig()

        assertTrue(config.enabled)
        assertEquals(ApiMirageDiagnostics.LOGS, config.diagnostics)
    }
}

