import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.7.10"
}

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
        binaries {
            executable {
                entryPoint = "com.icemachined.main"
            }
        }
    }
    sourceSets {
        val commonMain by getting

        val nativeMain by getting {
            dependencies {
                api(projects.kafkaClient)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val nativeTest by getting
    }

}

application {
    mainClass.set("com.icemachined.MainKt")
}
