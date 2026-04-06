package com.apimirage.core.fake

/**
 * Infers semantic hints from field names such as `email`, `createdAt`, or `user_id`.
 */
public object ApiMirageValueHintResolver {
    public fun inferFromName(name: String?): Set<ApiMirageValueHint> {
        if (name.isNullOrBlank()) {
            return emptySet()
        }

        val tokens = tokenize(name)
        val normalized = tokens.joinToString(separator = "")
        val hints = linkedSetOf<ApiMirageValueHint>()

        if ("id" in tokens || normalized.endsWith("id")) {
            hints += ApiMirageValueHint.ID
        }
        if ("name" in tokens || normalized.endsWith("name")) {
            hints += ApiMirageValueHint.NAME
        }
        if ("email" in tokens || normalized.contains("email")) {
            hints += ApiMirageValueHint.EMAIL
        }
        if ("phone" in tokens || "mobile" in tokens || "tel" in tokens) {
            hints += ApiMirageValueHint.PHONE
        }
        if ("url" in tokens || "uri" in tokens || "link" in tokens || "website" in tokens) {
            hints += ApiMirageValueHint.URL
        }
        if ("title" in tokens) {
            hints += ApiMirageValueHint.TITLE
        }
        if ("description" in tokens || "summary" in tokens || "bio" in tokens) {
            hints += ApiMirageValueHint.DESCRIPTION
        }
        if (isCreatedTimestamp(tokens, normalized)) {
            hints += ApiMirageValueHint.CREATED_AT
            hints += ApiMirageValueHint.DATE_TIME
        }
        if (isUpdatedTimestamp(tokens, normalized)) {
            hints += ApiMirageValueHint.UPDATED_AT
            hints += ApiMirageValueHint.DATE_TIME
        }
        if (
            "date" in tokens ||
            "time" in tokens ||
            "timestamp" in tokens ||
            normalized.endsWith("at")
        ) {
            hints += ApiMirageValueHint.DATE_TIME
        }

        return hints
    }

    private fun tokenize(name: String): List<String> {
        return name
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("[^A-Za-z0-9]+"), " ")
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
    }

    private fun isCreatedTimestamp(tokens: List<String>, normalized: String): Boolean {
        return normalized == "createdat" || ("created" in tokens && "at" in tokens)
    }

    private fun isUpdatedTimestamp(tokens: List<String>, normalized: String): Boolean {
        return normalized == "updatedat" || ("updated" in tokens && "at" in tokens)
    }
}

