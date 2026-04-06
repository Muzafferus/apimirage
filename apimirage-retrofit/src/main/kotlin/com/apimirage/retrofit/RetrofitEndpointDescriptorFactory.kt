package com.apimirage.retrofit

import com.apimirage.core.hooks.ApiMirageEndpointDescriptor

internal class RetrofitEndpointDescriptorFactory {
    fun create(invocation: RetrofitInvocationContext): ApiMirageEndpointDescriptor {
        return ApiMirageEndpointDescriptor(
            adapterName = "retrofit",
            serviceName = invocation.service.name,
            operationName = invocation.method.name,
            httpMethod = invocation.request?.method,
            path = invocation.request?.url?.encodedPath,
            arguments = invocation.method.parameters
                .mapIndexed { index, parameter ->
                    val name = parameter.name.takeUnless { it.isNullOrBlank() } ?: "arg$index"
                    name to invocation.arguments.getOrNull(index)
                }
                .toMap(),
        )
    }
}

internal fun RetrofitInvocationContext.describeForLogs(): String {
    val url = request?.url?.toString().orEmpty()
    return "${request?.method ?: "REQUEST"} $url".trim()
}

internal fun ApiMirageEndpointDescriptor.stableSeedKey(): String {
    return listOfNotNull(adapterName, serviceName, operationName, httpMethod, path)
        .joinToString(separator = "|")
        .ifBlank { "retrofit|unknown" }
}
