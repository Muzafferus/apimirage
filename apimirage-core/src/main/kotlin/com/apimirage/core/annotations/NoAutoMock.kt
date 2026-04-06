package com.apimirage.core.annotations

/**
 * Opts a service or endpoint out of automatic mock generation.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NoAutoMock

