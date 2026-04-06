package com.apimirage.core.generation

/**
 * Tracks the nested location currently being generated.
 */
public data class ApiMirageValuePath(
    val segments: List<String> = emptyList(),
) {
    public fun child(segment: String): ApiMirageValuePath {
        require(segment.isNotBlank()) { "segment must not be blank" }
        return ApiMirageValuePath(segments + segment)
    }

    public fun index(position: Int): ApiMirageValuePath {
        require(position >= 0) { "position must be non-negative" }
        return ApiMirageValuePath(segments + "[$position]")
    }

    override fun toString(): String {
        if (segments.isEmpty()) {
            return "$"
        }

        return buildString {
            append('$')
            segments.forEach { segment ->
                if (segment.startsWith("[")) {
                    append(segment)
                } else {
                    append('.')
                    append(segment)
                }
            }
        }
    }

    public companion object {
        public fun root(): ApiMirageValuePath = ApiMirageValuePath()
    }
}

