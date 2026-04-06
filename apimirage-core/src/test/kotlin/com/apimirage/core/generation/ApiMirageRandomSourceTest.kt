package com.apimirage.core.generation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageRandomSourceTest {
    @Test
    public fun `same seed produces same sequence`() {
        val first = ApiMirageRandomSource.fromSeed(42L)
        val second = ApiMirageRandomSource.fromSeed(42L)

        assertEquals(first.nextBoolean(), second.nextBoolean())
        assertEquals(first.nextInt(10_000), second.nextInt(10_000))
        assertEquals(first.nextAlphaNumeric(8), second.nextAlphaNumeric(8))
    }

    @Test
    public fun `fork is deterministic for the same label`() {
        val firstFork = ApiMirageRandomSource.fromSeed(123L).fork("profile")
        val secondFork = ApiMirageRandomSource.fromSeed(123L).fork("profile")
        val differentFork = ApiMirageRandomSource.fromSeed(123L).fork("address")

        assertEquals(firstFork.nextInt(10_000), secondFork.nextInt(10_000))
        assertNotEquals(firstFork.nextInt(10_000), differentFork.nextInt(10_000))
    }

    @Test
    public fun `nextFrom picks an item from a non-empty list`() {
        val random = ApiMirageRandomSource.fromSeed(7L)
        val value = random.nextFrom(listOf("alpha", "beta", "gamma"))

        assertTrue(value in listOf("alpha", "beta", "gamma"))
    }
}

