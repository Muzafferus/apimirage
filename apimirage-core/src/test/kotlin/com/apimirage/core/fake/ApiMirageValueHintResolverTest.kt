package com.apimirage.core.fake

import com.apimirage.core.annotations.ApiMirageFieldHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class ApiMirageValueHintResolverTest {
    @Test
    public fun `resolver infers common semantic hints from field names`() {
        assertEquals(setOf(ApiMirageValueHint.EMAIL), ApiMirageValueHintResolver.inferFromName("email"))
        assertTrue(ApiMirageValueHintResolver.inferFromName("user_id").contains(ApiMirageValueHint.ID))
        assertEquals(
            setOf(ApiMirageValueHint.CREATED_AT, ApiMirageValueHint.DATE_TIME),
            ApiMirageValueHintResolver.inferFromName("createdAt"),
        )
    }

    @Test
    public fun `property context combines annotation and inferred hints`() {
        val annotation = AnnotatedFieldHolder::class.java
            .getDeclaredField("contact")
            .annotations
            .filterIsInstance<ApiMirageFieldHint>()
            .first()

        val context = ApiMiragePropertyContext.from(
            declaredName = "contact",
            annotations = listOf(annotation),
        )

        assertTrue(context.hints.contains(ApiMirageValueHint.PHONE))
    }

    private data class AnnotatedFieldHolder(
        @field:ApiMirageFieldHint(ApiMirageValueHint.PHONE)
        val contact: String = "",
    )
}

