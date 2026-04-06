package com.apimirage.retrofit

import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.annotations.NoAutoMock
import com.apimirage.core.generation.ApiMirageGenerationRequest
import com.apimirage.core.generation.ApiMirageGenerationResult
import com.apimirage.core.generation.ApiMirageMockGenerator
import com.apimirage.core.generation.DefaultApiMirageMockGenerator
import com.apimirage.core.hooks.ApiMirageExtensionRegistry
import com.apimirage.core.newRandomSource
import com.apimirage.core.serialization.ApiMirageJsonEncoder
import com.apimirage.core.serialization.ApiMirageJsonEncodingRequest
import com.apimirage.core.serialization.ApiMirageJsonEncodingResult
import com.apimirage.core.serialization.DefaultApiMirageJsonEncoder
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp application interceptor that turns Retrofit requests into synthetic 200 JSON responses
 * when ApiMirage is enabled.
 */
public class ApiMirageInterceptor internal constructor(
    private val configProvider: () -> ApiMirageConfig = { ApiMirage.currentConfig() },
    private val invocationReader: RetrofitInvocationReader = RetrofitInvocationReader(),
    private val responseTypeResolver: RetrofitResponseTypeResolver = RetrofitResponseTypeResolver(invocationReader),
    private val mockGenerator: ApiMirageMockGenerator = DefaultApiMirageMockGenerator(),
    private val jsonEncoder: ApiMirageJsonEncoder = DefaultApiMirageJsonEncoder(),
    private val syntheticResponseFactory: RetrofitSyntheticResponseFactory = RetrofitSyntheticResponseFactory(),
    private val endpointDescriptorFactory: RetrofitEndpointDescriptorFactory = RetrofitEndpointDescriptorFactory(),
    private val logger: ApiMirageRetrofitLogger = JvmApiMirageRetrofitLogger,
    private val extensionsProvider: () -> ApiMirageExtensionRegistry = { ApiMirage.currentExtensions() },
) : Interceptor {
    public constructor() : this(configProvider = { ApiMirage.currentConfig() })

    public constructor(enabled: Boolean) : this(
        configProvider = { ApiMirage.currentConfig().copy(enabled = enabled) },
    )

    public constructor(config: ApiMirageConfig) : this(
        configProvider = { config },
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val config = configProvider()
        val requestLabel = request.describeForLogs()

        if (!config.enabled) {
            log(config, "Pass-through $requestLabel because ApiMirage is disabled.")
            return chain.proceed(request)
        }

        val invocationContext = invocationReader.read(request)
        if (invocationContext == null) {
            log(config, "Pass-through $requestLabel because Retrofit Invocation metadata is missing.")
            return chain.proceed(request)
        }

        if (shouldPassThrough(invocationContext)) {
            log(
                config,
                "Pass-through ${invocationContext.describeForLogs()} because @NoAutoMock is present.",
            )
            return chain.proceed(request)
        }

        val resolution = responseTypeResolver.resolve(invocationContext)
        if (resolution !is RetrofitResponseTypeResolution.Success) {
            val reason = (resolution as RetrofitResponseTypeResolution.Unsupported).reason
            log(config, "Pass-through ${invocationContext.describeForLogs()} because $reason")
            return chain.proceed(request)
        }

        val endpoint = endpointDescriptorFactory.create(resolution.invocation)
        val typeLabel = resolution.resolvedType.bodyType.describe()
        log(config, "Resolved response model $typeLabel for ${resolution.invocation.describeForLogs()}.")

        val generationRequest = ApiMirageGenerationRequest(
            targetType = resolution.resolvedType.bodyType,
            random = config.newRandomSource().fork(endpoint.stableSeedKey()),
            endpoint = endpoint,
            extensions = extensionsProvider(),
        )

        return when (val generation = mockGenerator.generate(generationRequest)) {
            is ApiMirageGenerationResult.Success -> encodeAndRespond(
                chain = chain,
                config = config,
                resolution = resolution,
                generatedValue = generation.value,
            )

            is ApiMirageGenerationResult.PassThrough -> {
                log(
                    config,
                    "Pass-through ${resolution.invocation.describeForLogs()} because mock generation failed: ${generation.reason}",
                )
                chain.proceed(request)
            }

            is ApiMirageGenerationResult.Diagnostic -> {
                log(
                    config,
                    "Diagnostic for ${resolution.invocation.describeForLogs()}: ${generation.message} at ${generation.path}",
                )
                chain.proceed(request)
            }
        }
    }

    private fun encodeAndRespond(
        chain: Interceptor.Chain,
        config: ApiMirageConfig,
        resolution: RetrofitResponseTypeResolution.Success,
        generatedValue: Any?,
    ): Response {
        val encoding = jsonEncoder.encode(
            ApiMirageJsonEncodingRequest(
                value = generatedValue,
                targetType = resolution.resolvedType.bodyType,
                diagnostics = config.diagnostics,
            ),
        )

        return when (encoding) {
            is ApiMirageJsonEncodingResult.Success -> {
                log(
                    config,
                    "Mocked ${resolution.invocation.describeForLogs()} as ${resolution.resolvedType.bodyType.describe()} using ${encoding.strategy.name}.",
                )
                syntheticResponseFactory.create(
                    chain = chain,
                    json = encoding.json,
                )
            }

            is ApiMirageJsonEncodingResult.PassThrough -> {
                log(
                    config,
                    "Pass-through ${resolution.invocation.describeForLogs()} because JSON encoding failed: ${encoding.reason}",
                )
                chain.proceed(chain.request())
            }

            is ApiMirageJsonEncodingResult.Diagnostic -> {
                log(
                    config,
                    "Diagnostic for ${resolution.invocation.describeForLogs()}: ${encoding.message} at ${encoding.path}",
                )
                chain.proceed(chain.request())
            }
        }
    }

    private fun shouldPassThrough(invocation: RetrofitInvocationContext): Boolean {
        return invocation.method.isAnnotationPresent(NoAutoMock::class.java) ||
            invocation.service.isAnnotationPresent(NoAutoMock::class.java)
    }

    private fun log(config: ApiMirageConfig, message: String) {
        logger.log(config, message)
    }
}
