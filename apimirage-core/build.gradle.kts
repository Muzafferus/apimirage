import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.apimirage.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
}

val centralPortalUsername = providers.gradleProperty("centralPortalUsername")
    .orElse(providers.environmentVariable("CENTRAL_USERNAME"))
    .map { it.trim() }
val centralPortalPassword = providers.gradleProperty("centralPortalPassword")
    .orElse(providers.environmentVariable("CENTRAL_PASSWORD"))
    .map { it.trim() }
val signingKey = providers.gradleProperty("signingKey")
    .orElse(providers.environmentVariable("SIGNING_KEY"))
val signingPassword = providers.gradleProperty("signingPassword")
    .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
    .map { it.trim() }
val isCentralPublishRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("CentralPortal", ignoreCase = true) ||
        taskName.contains("closeAndReleaseCentralBundle", ignoreCase = true)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(rootProject.file("README.md"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = project.group.toString()
                artifactId = "apimirage-core"
                version = project.version.toString()

                from(components["release"])
                artifact(javadocJar.get())

                pom {
                    name.set("ApiMirage Core")
                    description.set(
                        "Adapter-agnostic core engine for ApiMirage deterministic Android API mocking.",
                    )
                    url.set(providers.gradleProperty("POM_URL").get())

                    licenses {
                        license {
                            name.set(providers.gradleProperty("POM_LICENSE_NAME").get())
                            url.set(providers.gradleProperty("POM_LICENSE_URL").get())
                        }
                    }

                    developers {
                        developer {
                            id.set(providers.gradleProperty("POM_DEVELOPER_ID").get())
                            name.set(providers.gradleProperty("POM_DEVELOPER_NAME").get())
                            url.set(providers.gradleProperty("POM_DEVELOPER_URL").get())
                        }
                    }

                    scm {
                        url.set(providers.gradleProperty("POM_SCM_URL").get())
                        connection.set(providers.gradleProperty("POM_SCM_CONNECTION").get())
                        developerConnection.set(
                            providers.gradleProperty("POM_SCM_DEV_CONNECTION").get(),
                        )
                    }
                }
            }
        }

        repositories {
            maven {
                name = "CentralPortal"
                url = uri(
                    "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/",
                )
                credentials {
                    username = centralPortalUsername.orNull
                    password = centralPortalPassword.orNull
                }
            }
        }
    }

    signing {
        isRequired = isCentralPublishRequested

        val key = signingKey.orNull
        val password = signingPassword.orNull
        if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
            useInMemoryPgpKeys(key, password)
            sign(publishing.publications["release"])
        }
    }
}
