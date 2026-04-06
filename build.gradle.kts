import java.net.HttpURLConnection
import java.util.Base64

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    group = providers.gradleProperty("POM_GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

tasks.register("closeAndReleaseCentralBundle") {
    group = "publishing"
    description = "Uploads the Maven-like staging deployment to the Central Publisher Portal."

    doLast {
        val namespace = providers.gradleProperty("POM_GROUP").get()
        val username = providers.gradleProperty("centralPortalUsername")
            .orElse(providers.environmentVariable("CENTRAL_USERNAME"))
            .map { it.trim() }
            .orNull
            ?: error("Missing Central Portal username. Set centralPortalUsername or CENTRAL_USERNAME.")
        val password = providers.gradleProperty("centralPortalPassword")
            .orElse(providers.environmentVariable("CENTRAL_PASSWORD"))
            .map { it.trim() }
            .orNull
            ?: error("Missing Central Portal password. Set centralPortalPassword or CENTRAL_PASSWORD.")

        val authToken = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
        val endpoint =
            "https://ossrh-staging-api.central.sonatype.com/service/local/manual/upload/defaultRepository/$namespace" +
                "?publishing_type=automatic"

        val connection = java.net.URI(endpoint).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $authToken")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.outputStream.use { }

        val responseCode = connection.responseCode
        val responseBody = (if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        })?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            error(
                "Central manual upload failed with HTTP $responseCode.\n$responseBody",
            )
        }

        logger.lifecycle(
            "Central upload accepted for namespace $namespace. Response: ${responseBody.ifBlank { "<empty>" }}",
        )
    }
}
