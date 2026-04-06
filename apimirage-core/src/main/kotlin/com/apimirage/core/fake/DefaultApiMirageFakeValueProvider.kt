package com.apimirage.core.fake

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Default field-aware fake value provider for the core generator.
 */
public class DefaultApiMirageFakeValueProvider : ApiMirageFakeValueProvider {
    override fun provide(request: ApiMirageFakeValueRequest): ApiMirageFakeValueResult {
        val rawClass = request.targetType.rawClass ?: return ApiMirageFakeValueResult.Unhandled

        val value = when (rawClass) {
            String::class -> provideString(request)
            Int::class -> provideInt(request)
            Long::class -> provideLong(request)
            Short::class -> provideInt(request).toShort()
            Byte::class -> provideInt(request).toByte()
            Double::class -> provideDouble(request)
            Float::class -> provideDouble(request).toFloat()
            Boolean::class -> provideBoolean(request)
            else -> null
        } ?: return ApiMirageFakeValueResult.Unhandled

        return ApiMirageFakeValueResult.Provided(value)
    }

    private fun provideString(request: ApiMirageFakeValueRequest): String {
        val hints = request.property.hints

        return when {
            ApiMirageValueHint.EMAIL in hints -> buildEmail(request)
            ApiMirageValueHint.PHONE in hints -> buildPhone(request)
            ApiMirageValueHint.URL in hints -> buildUrl(request)
            ApiMirageValueHint.CREATED_AT in hints -> buildIsoTimestamp(request, daysBack = 14)
            ApiMirageValueHint.UPDATED_AT in hints -> buildIsoTimestamp(request, daysBack = 2)
            ApiMirageValueHint.DATE_TIME in hints -> buildIsoTimestamp(request, daysBack = 7)
            ApiMirageValueHint.TITLE in hints -> TITLES[request.random.nextInt(TITLES.size)]
            ApiMirageValueHint.DESCRIPTION in hints -> DESCRIPTIONS[request.random.nextInt(DESCRIPTIONS.size)]
            ApiMirageValueHint.NAME in hints -> buildName(request)
            ApiMirageValueHint.ID in hints -> buildStringId(request)
            else -> buildGenericString(request)
        }
    }

    private fun provideInt(request: ApiMirageFakeValueRequest): Int {
        return if (ApiMirageValueHint.ID in request.property.hints) {
            1_000 + request.random.nextInt(900_000)
        } else {
            10 + request.random.nextInt(9_990)
        }
    }

    private fun provideLong(request: ApiMirageFakeValueRequest): Long {
        return if (ApiMirageValueHint.ID in request.property.hints) {
            10_000L + request.random.nextInt(9_000_000).toLong()
        } else {
            100L + request.random.nextInt(90_000).toLong()
        }
    }

    private fun provideDouble(request: ApiMirageFakeValueRequest): Double {
        val whole = 10 + request.random.nextInt(500)
        val decimal = request.random.nextInt(100)
        return whole + (decimal / 100.0)
    }

    private fun provideBoolean(request: ApiMirageFakeValueRequest): Boolean {
        val name = request.property.declaredName.orEmpty()
        return when {
            name.startsWith("is", ignoreCase = true) -> true
            name.startsWith("has", ignoreCase = true) -> request.random.nextBoolean()
            else -> request.random.nextBoolean()
        }
    }

    private fun buildName(request: ApiMirageFakeValueRequest): String {
        val first = FIRST_NAMES[request.random.nextInt(FIRST_NAMES.size)]
        val last = LAST_NAMES[request.random.nextInt(LAST_NAMES.size)]
        return "$first $last"
    }

    private fun buildEmail(request: ApiMirageFakeValueRequest): String {
        val first = FIRST_NAMES[request.random.nextInt(FIRST_NAMES.size)].lowercase()
        val last = LAST_NAMES[request.random.nextInt(LAST_NAMES.size)].lowercase()
        val suffix = 10 + request.random.nextInt(90)
        return "$first.$last$suffix@example.com"
    }

    private fun buildPhone(request: ApiMirageFakeValueRequest): String {
        val exchange = 200 + request.random.nextInt(700)
        val suffix = 1_000 + request.random.nextInt(9_000)
        return "+1-$exchange-555-$suffix"
    }

    private fun buildUrl(request: ApiMirageFakeValueRequest): String {
        val path = slug(request.property.declaredName ?: "resource")
        val token = request.random.nextAlphaNumeric(6).lowercase()
        return "https://example.com/$path/$token"
    }

    private fun buildStringId(request: ApiMirageFakeValueRequest): String {
        val prefix = slug(request.property.declaredName ?: "item")
        val token = request.random.nextAlphaNumeric(8).lowercase()
        return "${prefix}_$token"
    }

    private fun buildGenericString(request: ApiMirageFakeValueRequest): String {
        val adjective = ADJECTIVES[request.random.nextInt(ADJECTIVES.size)]
        val noun = NOUNS[request.random.nextInt(NOUNS.size)]
        val property = request.property.declaredName?.let(::slug)?.takeIf(String::isNotBlank)

        return listOfNotNull(property, adjective, noun)
            .joinToString(separator = "-")
            .replaceFirstChar { it.uppercase() }
    }

    private fun buildIsoTimestamp(
        request: ApiMirageFakeValueRequest,
        daysBack: Long,
    ): String {
        val offsetHours = abs(request.random.nextLong() % (daysBack * 24 + 1))
        return Instant.parse(BASE_INSTANT)
            .minus(offsetHours, ChronoUnit.HOURS)
            .toString()
    }

    private fun slug(raw: String): String {
        return raw
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .replace(Regex("[^A-Za-z0-9]+"), "-")
            .trim('-')
            .lowercase()
            .ifBlank { "value" }
    }

    private companion object {
        private const val BASE_INSTANT: String = "2026-04-07T12:00:00Z"

        private val FIRST_NAMES = listOf(
            "Avery",
            "Jordan",
            "Riley",
            "Taylor",
            "Cameron",
            "Morgan",
            "Casey",
            "Harper",
        )

        private val LAST_NAMES = listOf(
            "Nguyen",
            "Patel",
            "Rivera",
            "Brooks",
            "Kim",
            "Hayes",
            "Ortiz",
            "Coleman",
        )

        private val TITLES = listOf(
            "Quarterly Product Update",
            "Customer Success Spotlight",
            "Platform Health Summary",
            "Release Planning Brief",
            "Field Operations Snapshot",
        )

        private val DESCRIPTIONS = listOf(
            "Detailed summary prepared for internal review and planning.",
            "Useful context that mirrors a realistic backend response body.",
            "Narrative description with enough substance to exercise UI states.",
            "Concise explanation that still feels like production data.",
            "Generated content intended for deterministic debug workflows.",
        )

        private val ADJECTIVES = listOf(
            "steady",
            "bright",
            "trusted",
            "coastal",
            "modern",
            "signal",
            "prime",
            "atlas",
        )

        private val NOUNS = listOf(
            "record",
            "profile",
            "summary",
            "listing",
            "preview",
            "payload",
            "channel",
            "report",
        )
    }
}

