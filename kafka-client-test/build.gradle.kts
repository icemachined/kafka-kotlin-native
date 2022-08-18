import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.7.10"
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    //val os = getCurrentOperatingSystem()

    //registerNativeBinaries(os, this)

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "com.icemachined.main"
            }
        }
    }
    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(projects.kafkaClient)
            }
        }
        // val linuxMain by creating {
        val nativeMain by getting {
            dependencies {
                api(projects.kafkaClient)
            }
        }
        val nativeTest by getting
    }

}

application {
    mainClass.set("com.icemachined.MainKt")
}
