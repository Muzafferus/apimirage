package com.apimirage.sample.model

import kotlinx.serialization.Serializable

@Serializable
public data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val url: String,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
public data class BaseResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T,
)
