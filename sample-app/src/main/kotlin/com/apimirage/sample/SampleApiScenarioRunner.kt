package com.apimirage.sample

import com.apimirage.core.ApiMirageConfig
import com.apimirage.sample.model.BaseResponse
import com.apimirage.sample.model.UserDto
import com.apimirage.sample.network.SampleApiService
import retrofit2.Call
import retrofit2.Response

public class SampleApiScenarioRunner(
    private val service: SampleApiService,
) {
    public fun runDemoReport(config: ApiMirageConfig): String {
        return runCatching {
            val user = service.getUser().execute().requireBody("UserDto")
            val users = service.getUsers().execute().requireBody("List<UserDto>")
            val wrapped = service.getWrappedUser().execute().requireBody("BaseResponse<UserDto>")

            buildString {
                appendLine("ApiMirage Sample Flow")
                appendLine("enabled=${config.enabled}")
                appendLine("seed=${config.seed ?: "none"}")
                appendLine("diagnostics=${config.diagnostics}")
                appendLine()
                appendLine("1. UserDto")
                appendLine("name=${user.name}")
                appendLine("email=${user.email}")
                appendLine("createdAt=${user.createdAt}")
                appendLine()
                appendLine("2. List<UserDto>")
                appendLine("count=${users.size}")
                users.take(3).forEachIndexed { index, item ->
                    appendLine("${index + 1}. ${item.name} (${item.email})")
                }
                appendLine()
                appendLine("3. BaseResponse<UserDto>")
                appendLine("success=${wrapped.success}")
                appendLine("message=${wrapped.message}")
                appendLine("nestedUser=${wrapped.data.name}")
            }.trimEnd()
        }.getOrElse { throwable ->
            buildString {
                appendLine("ApiMirage Sample Flow")
                appendLine("enabled=${config.enabled}")
                appendLine("seed=${config.seed ?: "none"}")
                appendLine("diagnostics=${config.diagnostics}")
                appendLine()
                appendLine("Sample request failed.")
                appendLine(throwable.message ?: throwable::class.java.name)
            }.trimEnd()
        }
    }

    private fun <T : Any> Response<T>.requireBody(label: String): T {
        if (!isSuccessful) {
            error("$label request failed with HTTP ${code()}")
        }
        return body() ?: error("$label request returned an empty body")
    }
}
