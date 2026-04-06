package com.apimirage.sample.network

import com.apimirage.sample.model.BaseResponse
import com.apimirage.sample.model.UserDto
import retrofit2.Call
import retrofit2.http.GET

public interface SampleApiService {
    @GET("sample/user")
    public fun getUser(): Call<UserDto>

    @GET("sample/users")
    public fun getUsers(): Call<List<UserDto>>

    @GET("sample/wrapped-user")
    public fun getWrappedUser(): Call<BaseResponse<UserDto>>
}

