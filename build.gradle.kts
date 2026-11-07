import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType").map { IntelliJPlatformType.fromCode(it) },
            providers.gradleProperty("platformVersion"),
        )
        pluginVerifier()
        zipSigner()
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Intentionally no untilBuild: the plugin uses only stable platform APIs.
            untilBuild = provider { null }
        }

        // Release notes shown on the Marketplace page, taken from the newest CHANGELOG section.
        changeNotes = provider {
            val text = layout.projectDirectory.file("CHANGELOG.md").asFile.readText()
            val section = Regex("(?s)## \\[?([0-9][^\\]\\s]*)\\]?[^\\n]*\\n(.*?)(?=\\n## |\\z)")
                .find(text)?.groupValues?.get(2)?.trim().orEmpty()
            section.lines().joinToString("") { line ->
                when {
                    line.startsWith("- ") -> "<li>${line.removePrefix("- ")}</li>"
                    line.isBlank() -> ""
                    else -> "<p>$line</p>"
                }
            }.let { if (it.contains("<li>")) "<ul>$it</ul>" else it }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // Use "eap" or "beta" here to publish to a pre-release channel.
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            // Verify against the oldest supported build and a recent one.
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2023.3.8")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.5")
            // Android Studio (Ladybug) so the Android Studio target is checked explicitly.
            ide(IntelliJPlatformType.AndroidStudio, "2024.2.1.11")
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "8.10.2"
    }
    test {
        useJUnit()
    }
}
