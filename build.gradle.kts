import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import kotlin.io.path.Path
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.nameWithoutExtension

plugins {
    alias(libs.plugins.multiplatform)
}

val cInterop = "src/nativeInterop/cInterop"

kotlin {
    jvm()

    linuxArm64 {
        compilations["main"].cinterops {
            // https://kotlinlang.org/docs/native-c-interop.html
            // https://kotlinlang.org/docs/native-app-with-c-and-libcurl.html
            Path(cInterop).forEachDirectoryEntry(glob = "*.def") { create(it.nameWithoutExtension) }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        explicitApi()
        allWarningsAsErrors = true
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// see https://youtrack.jetbrains.com/issue/KT-43996
tasks.named("linkDebugTestLinuxArm64", type = KotlinNativeLink::class) {
    binary.linkerOpts("-L$cInterop/libs")
}

tasks.named("build") {
    dependsOn("linkDebugTestLinuxArm64")
}

repositories {
    mavenCentral()
}
