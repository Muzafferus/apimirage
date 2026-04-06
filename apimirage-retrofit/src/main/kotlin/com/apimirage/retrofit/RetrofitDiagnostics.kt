package com.apimirage.retrofit

import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import java.util.logging.Logger
import okhttp3.Request

internal fun Request.describeForLogs(): String = "${method} ${url}"

internal fun com.apimirage.core.generation.ApiMirageTypeKey.describe(): String {
    val name = rawClass?.simpleName ?: rawTypeName.substringAfterLast('.')
    if (arguments.isEmpty()) {
        return name
    }
    return "$name<${arguments.joinToString(separator = ", ") { it.describe() }}>"
}

internal fun interface ApiMirageRetrofitLogger {
    fun log(config: ApiMirageConfig, message: String)
}

internal object JvmApiMirageRetrofitLogger : ApiMirageRetrofitLogger {
    private val logger: Logger = Logger.getLogger("ApiMirage")

    override fun log(config: ApiMirageConfig, message: String) {
        if (config.diagnostics != ApiMirageDiagnostics.LOGS) {
            return
        }
        logger.info("[ApiMirage] $message")
    }
}
