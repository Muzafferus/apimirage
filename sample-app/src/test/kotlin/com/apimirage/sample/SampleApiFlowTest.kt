package com.apimirage.sample

import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.sample.network.SampleApiEnvironment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class SampleApiFlowTest {
    @After
    public fun tearDown() {
        ApiMirage.clearCustomizations()
        ApiMirage.install(ApiMirageConfig())
    }

    @Test
    public fun `sample retrofit flow returns parsed UserDto List and BaseResponse`() {
        ApiMirage.install(
            ApiMirageConfig(
                enabled = true,
                seed = 20260407L,
                diagnostics = ApiMirageDiagnostics.NONE,
            ),
        )
        val service = SampleApiEnvironment.createService()

        val userResponse = service.getUser().execute()
        val listResponse = service.getUsers().execute()
        val wrappedResponse = service.getWrappedUser().execute()

        assertTrue(userResponse.isSuccessful)
        assertTrue(listResponse.isSuccessful)
        assertTrue(wrappedResponse.isSuccessful)

        val user = userResponse.body()
        val users = listResponse.body()
        val wrapped = wrappedResponse.body()

        assertNotNull(user)
        assertNotNull(users)
        assertNotNull(wrapped)
        assertTrue(user!!.name.isNotBlank())
        assertTrue(users!!.size >= 3)
        assertEquals(true, wrapped!!.success)
        assertNotNull(wrapped.data)
        assertTrue(wrapped.data.name.isNotBlank())
    }
}
