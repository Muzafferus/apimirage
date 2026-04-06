package com.apimirage.core.serialization

import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.core.generation.ApiMirageTypeKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class DefaultApiMirageJsonEncoderTest {
    private val encoder = DefaultApiMirageJsonEncoder()
    private val parser = Json { ignoreUnknownKeys = false }

    @Test
    public fun `encoder prefers kotlinx serialization for serializable models`() {
        val request = ApiMirageJsonEncodingRequest(
            value = SerializableUser(id = "user_1", name = "Avery Nguyen"),
            targetType = ApiMirageTypeKey.fromRawClass(SerializableUser::class),
        )

        val result = encoder.encode(request) as ApiMirageJsonEncodingResult.Success

        assertEquals(ApiMirageJsonEncodingStrategy.KOTLINX_SERIALIZATION, result.strategy)
        val payload = parser.parseToJsonElement(result.json).toString()
        assertTrue(payload.contains("user_1"))
        assertTrue(payload.contains("Avery Nguyen"))
    }

    @Test
    public fun `encoder supports generic serializable wrappers through kotlinx`() {
        val request = ApiMirageJsonEncodingRequest(
            value = SerializableEnvelope(
                data = SerializableUser(id = "user_7", name = "Jordan Kim"),
                items = listOf(SerializableUser(id = "user_8", name = "Taylor Brooks")),
            ),
            targetType = ApiMirageTypeKey.fromRawClass(
                SerializableEnvelope::class,
                arguments = listOf(ApiMirageTypeKey.fromRawClass(SerializableUser::class)),
            ),
        )

        val result = encoder.encode(request) as ApiMirageJsonEncodingResult.Success

        assertEquals(ApiMirageJsonEncodingStrategy.KOTLINX_SERIALIZATION, result.strategy)
        val payload = parser.parseToJsonElement(result.json).toString()
        assertTrue(payload.contains("user_7"))
        assertTrue(payload.contains("items"))
    }

    @Test
    public fun `encoder falls back to reflection for supported non-serializable data classes`() {
        val request = ApiMirageJsonEncodingRequest(
            value = ReflectiveUser(
                id = 42,
                name = "Riley Patel",
                tags = listOf("android", "debug"),
                profile = mapOf("city" to "Baku"),
            ),
            targetType = ApiMirageTypeKey.fromRawClass(ReflectiveUser::class),
        )

        val result = encoder.encode(request) as ApiMirageJsonEncodingResult.Success

        assertEquals(ApiMirageJsonEncodingStrategy.REFLECTION_FALLBACK, result.strategy)
        val payload = parser.parseToJsonElement(result.json).toString()
        assertTrue(payload.contains("Riley Patel"))
        assertTrue(payload.contains("android"))
        assertTrue(payload.contains("Baku"))
    }

    @Test
    public fun `encoder passes through unsupported reflective shapes when diagnostics are off`() {
        val request = ApiMirageJsonEncodingRequest(
            value = UnsupportedShape(),
            targetType = ApiMirageTypeKey.fromRawClass(UnsupportedShape::class),
            diagnostics = ApiMirageDiagnostics.NONE,
        )

        val result = encoder.encode(request)

        assertTrue(result is ApiMirageJsonEncodingResult.PassThrough)
    }

    @Test
    public fun `encoder returns diagnostics for unsupported reflective shapes when diagnostics are on`() {
        val request = ApiMirageJsonEncodingRequest(
            value = UnsupportedShape(),
            targetType = ApiMirageTypeKey.fromRawClass(UnsupportedShape::class),
            diagnostics = ApiMirageDiagnostics.LOGS,
        )

        val result = encoder.encode(request) as ApiMirageJsonEncodingResult.Diagnostic

        assertTrue(result.message.contains("Unsupported reflective JSON shape"))
    }

    @Serializable
    private data class SerializableUser(
        val id: String,
        val name: String,
    )

    @Serializable
    private data class SerializableEnvelope<T>(
        val data: T,
        val items: List<T>,
    )

    private data class ReflectiveUser(
        val id: Int,
        val name: String,
        val tags: List<String>,
        val profile: Map<String, String>,
    )

    private class UnsupportedShape
}
