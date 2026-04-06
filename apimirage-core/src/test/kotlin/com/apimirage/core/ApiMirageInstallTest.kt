package com.apimirage.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageInstallTest {
    @After
    public fun tearDown() {
        ApiMirage.resetForTesting()
    }

    @Test
    public fun `install with explicit enabled flag updates runtime state`() {
        ApiMirage.install(enabled = false)

        assertFalse(ApiMirage.isEnabled())
        assertEquals(ApiMirageConfig(enabled = false), ApiMirage.currentConfig())
    }

    @Test
    public fun `install with config keeps seed and diagnostics`() {
        val config = ApiMirageConfig(
            enabled = true,
            seed = 42L,
            diagnostics = ApiMirageDiagnostics.LOGS,
        )

        ApiMirage.install(config)

        assertEquals(config, ApiMirage.currentConfig())
        assertTrue(ApiMirage.isEnabled())
    }

    @Test
    public fun `install with no arguments resets to variant defaults`() {
        ApiMirage.install(
            ApiMirageConfig(
                enabled = false,
                seed = 7L,
                diagnostics = ApiMirageDiagnostics.NONE,
            ),
        )

        ApiMirage.install()

        val currentConfig = ApiMirage.currentConfig()
        assertEquals(ApiMirageVariantDefaults.enabledByDefault, currentConfig.enabled)
        assertEquals(ApiMirageVariantDefaults.diagnosticsByDefault, currentConfig.diagnostics)
        assertNull(currentConfig.seed)
    }
}

