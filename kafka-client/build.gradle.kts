import com.icemachined.buildutils.configureNuget
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("com.icemachined.buildutils.kotlin-library")
}

configureNuget()

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> if (DefaultNativePlatform.getCurrentArchitecture().isArm) macosArm64() else macosX64()
        hostOs == "Linux" -> linuxX64()
        isMingwX64 -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
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
