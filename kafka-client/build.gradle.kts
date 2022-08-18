import com.icemachined.buildutils.configureNuget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.7.10"
}

configureNuget()

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            // NL
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
//        binaries {
//            framework {
//            }
//        }
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
        // val linuxMain by creating {
        val nativeMain by getting {
            dependencies {
                // implementation(libs.kotlin.logger.linux)
            }
        }
        val nativeTest by getting
    }
}
