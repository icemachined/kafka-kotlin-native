import com.icemachined.buildutils.configureNuget
import org.jetbrains.kotlin.gradle.plugin.mpp.*


plugins {
    kotlin("multiplatform")
    id("com.icemachined.buildutils.kotlin-library")
}

configureNuget()

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    fun KotlinNativeTarget.config() {
        compilations.getByName("main") {
            cinterops {
                val librdkafka by getting {
                    if (isMingwX64) {
                        tasks.named(interopProcessingTaskName) { dependsOn("nugetRestore") }
                    } else {
                        logger.warn("To install librdkafka c library on linux or macos see https://github.com/edenhill/librdkafka#installation")
                    }
                }
            }
        }
    }
    macosArm64 { config() }
    macosX64 { config() }
    linuxX64 { config() }
    mingwX64 { config() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val nativeMain by getting
    }
}
