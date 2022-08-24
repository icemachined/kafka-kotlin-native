import com.icemachined.buildutils.configureNuget
import com.icemachined.buildutils.configurePublishing

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.7.10"
}

configureNuget()
configurePublishing()

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTargets = listOf(linuxX64(), mingwX64(), macosX64())

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64()
        hostOs == "Linux" -> linuxX64()
        isMingwX64 -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val librdkafka by creating {
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
                implementation(libs.kotlin.logger)
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
//            dependencies {
//                // implementation(libs.kotlin.logger.linux)
//            }
        }
        nativeTargets.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
    }
}
