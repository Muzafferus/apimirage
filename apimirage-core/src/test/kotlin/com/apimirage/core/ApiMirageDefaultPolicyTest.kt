package com.apimirage.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageDefaultPolicyTest {
    @Test
    public fun `debuggable builds default to enabled mocks with logs`() {
        val defaults = ApiMirageDefaultPolicy.forDebuggableBuild(isDebuggableBuild = true)

        assertTrue(defaults.enabled)
        assertEquals(ApiMirageDiagnostics.LOGS, defaults.diagnostics)
    }

    @Test
    public fun `non-debuggable builds default to pass-through with no diagnostics`() {
        val defaults = ApiMirageDefaultPolicy.forDebuggableBuild(isDebuggableBuild = false)

        assertFalse(defaults.enabled)
        assertEquals(ApiMirageDiagnostics.NONE, defaults.diagnostics)
    }
}

