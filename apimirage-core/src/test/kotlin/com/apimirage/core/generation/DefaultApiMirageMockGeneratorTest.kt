package com.apimirage.core.generation

import com.apimirage.core.fake.ApiMiragePropertyContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class DefaultApiMirageMockGeneratorTest {
    private val generator = DefaultApiMirageMockGenerator()

    @Test
    public fun `generator supports primitives strings and enums`() {
        val stringValue = generate(ApiMirageTypeKey.fromRawClass(String::class))
        val intValue = generate(ApiMirageTypeKey.fromRawClass(Int::class))
        val booleanValue = generate(ApiMirageTypeKey.fromRawClass(Boolean::class))
        val enumValue = generate(ApiMirageTypeKey.fromRawClass(Status::class))

        assertTrue(stringValue is String && stringValue.isNotBlank())
        assertTrue(intValue is Int)
        assertTrue(booleanValue is Boolean)
        assertTrue(enumValue is Status)
    }

    @Test
    public fun `generator supports nested data classes and heuristics`() {
        val user = generate(ApiMirageTypeKey.fromRawClass(UserDto::class)) as UserDto

        assertTrue(user.id > 0)
        assertTrue(user.name.isNotBlank())
        assertTrue(user.email.contains("@example.com"))
        assertTrue(user.phone.startsWith("+1-"))
        assertTrue(user.website.startsWith("https://"))
        assertTrue(user.createdAt.contains("T"))
        assertTrue(user.address.lineOne.isNotBlank())
        assertTrue(user.nickName != null)
        assertTrue(user.status in Status.entries)
    }

    @Test
    public fun `generator supports lists with at least three items`() {
        val users = generate(
            ApiMirageTypeKey.fromRawClass(
                List::class,
                arguments = listOf(ApiMirageTypeKey.fromRawClass(UserDto::class)),
            ),
        ) as List<*>

        assertTrue(users.size >= 3)
        assertTrue(users.all { it is UserDto })
    }

    @Test
    public fun `generator supports maps`() {
        val value = generate(
            ApiMirageTypeKey.fromRawClass(
                Map::class,
                arguments = listOf(
                    ApiMirageTypeKey.fromRawClass(String::class),
                    ApiMirageTypeKey.fromRawClass(AddressDto::class),
                ),
            ),
        ) as Map<*, *>

        assertFalse(value.isEmpty())
        assertTrue(value.keys.all { it is String })
        assertTrue(value.values.all { it is AddressDto })
    }

    @Test
    public fun `generator resolves generic data class arguments`() {
        @Suppress("UNCHECKED_CAST")
        val value = generate(
            ApiMirageTypeKey.fromRawClass(
                Envelope::class,
                arguments = listOf(ApiMirageTypeKey.fromRawClass(UserDto::class)),
            ),
        ) as Envelope<UserDto>

        assertTrue(value.data.name.isNotBlank())
        assertTrue(value.items.size >= 3)
    }

    @Test
    public fun `same seed produces deterministic nested objects`() {
        val first = generateUser(seed = 77L)
        val second = generateUser(seed = 77L)

        assertEquals(first, second)
    }

    @Test
    public fun `different seeds produce different nested objects`() {
        val first = generateUser(seed = 77L)
        val second = generateUser(seed = 78L)

        assertNotEquals(first, second)
    }

    @Test
    public fun `unsupported non-null types pass through safely`() {
        val result = generator.generate(
            ApiMirageGenerationRequest(
                targetType = ApiMirageTypeKey.fromRawClass(UnsupportedType::class),
                random = ApiMirageRandomSource.fromSeed(5L),
            ),
        )

        assertTrue(result is ApiMirageGenerationResult.PassThrough)
    }

    @Test
    public fun `nullable unsupported fields become null`() {
        val holder = generate(ApiMirageTypeKey.fromRawClass(NullableUnsupportedHolder::class))
            as NullableUnsupportedHolder

        assertEquals(null, holder.payload)
    }

    private fun generate(typeKey: ApiMirageTypeKey): Any? {
        val result = generator.generate(
            ApiMirageGenerationRequest(
                targetType = typeKey,
                random = ApiMirageRandomSource.fromSeed(42L),
                property = ApiMiragePropertyContext(),
            ),
        )

        return when (result) {
            is ApiMirageGenerationResult.Success -> result.value
            else -> error("Expected success but was $result")
        }
    }

    private fun generateUser(seed: Long): UserDto {
        return generator.generate(
            ApiMirageGenerationRequest(
                targetType = ApiMirageTypeKey.fromRawClass(UserDto::class),
                random = ApiMirageRandomSource.fromSeed(seed),
            ),
        ).let { it as ApiMirageGenerationResult.Success }.value as UserDto
    }

    data class UserDto(
        val id: Long,
        val name: String,
        val email: String,
        val phone: String,
        val website: String,
        val title: String,
        val description: String,
        val createdAt: String,
        val updatedAt: String?,
        val status: Status,
        val nickName: String?,
        val address: AddressDto,
        val tags: List<String>,
        val metadata: Map<String, String>,
    )

    data class AddressDto(
        val lineOne: String,
        val city: String,
        val postalCode: String,
    )

    data class Envelope<T>(
        val data: T,
        val items: List<T>,
        val success: Boolean,
    )

    data class NullableUnsupportedHolder(
        val payload: UnsupportedType?,
    )

    class UnsupportedType

    enum class Status {
        ACTIVE,
        INACTIVE,
        PENDING,
    }
}
