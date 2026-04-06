package com.apimirage.core.generation

import kotlin.random.Random

/**
 * Abstraction over seeded randomness so generated data can be deterministic when needed.
 */
public interface ApiMirageRandomSource {
    public val seed: Long?

    public val isDeterministic: Boolean
        get() = seed != null

    public fun nextBoolean(): Boolean

    public fun nextDouble(): Double

    public fun nextInt(bound: Int): Int

    public fun nextLong(): Long

    public fun nextAlphaNumeric(length: Int): String

    public fun fork(label: String): ApiMirageRandomSource

    public fun <T> nextFrom(values: List<T>): T {
        require(values.isNotEmpty()) { "values must not be empty" }
        return values[nextInt(values.size)]
    }

    public companion object {
        public fun fromSeed(seed: Long?): ApiMirageRandomSource {
            return DefaultApiMirageRandomSource(seed)
        }
    }
}

internal class DefaultApiMirageRandomSource(
    override val seed: Long?,
) : ApiMirageRandomSource {
    private val random: Random = Random(seed ?: System.nanoTime())

    override fun nextBoolean(): Boolean = random.nextBoolean()

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be greater than zero" }
        return random.nextInt(bound)
    }

    override fun nextLong(): Long = random.nextLong()

    override fun nextAlphaNumeric(length: Int): String {
        require(length > 0) { "length must be greater than zero" }

        return buildString(length) {
            repeat(length) {
                append(ALPHANUMERIC[nextInt(ALPHANUMERIC.length)])
            }
        }
    }

    override fun fork(label: String): ApiMirageRandomSource {
        require(label.isNotBlank()) { "label must not be blank" }

        val forkSeed = seed
            ?.let { mix(it, label.hashCode().toLong()) }
            ?: mix(nextLong(), label.hashCode().toLong())

        return DefaultApiMirageRandomSource(forkSeed)
    }

    private fun mix(left: Long, right: Long): Long {
        val first = java.lang.Long.rotateLeft(left xor (right * MIXER_ONE), 17)
        return first * MIXER_TWO
    }

    private companion object {
        private const val ALPHANUMERIC: String =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val MIXER_ONE: Long = -7046029254386353131L
        private const val MIXER_TWO: Long = -4658895280553007687L
    }
}

