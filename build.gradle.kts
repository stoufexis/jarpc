import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.8.0" apply false
}

subprojects {
    plugins.apply("java")
    plugins.apply("com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    configure<SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            importOrder()
            trimTrailingWhitespace()
            endWithNewline()
            target("src/*/java/**/*.java")
            targetExclude("**/sample/generated/**")
        }
    }

    repositories {
        mavenCentral()
    }
}