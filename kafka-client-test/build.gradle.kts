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
    linkProperExecutable(getCurrentOperatingSystem())
}
/**
 * @param os
 * @throws GradleException
 */
fun linkProperExecutable(os: org.gradle.nativeplatform.platform.internal.DefaultOperatingSystem) {
    val linkReleaseExecutableTaskProvider = when {
        os.isLinux -> tasks.getByName("linkReleaseExecutableLinuxX64")
        os.isWindows -> tasks.getByName("linkReleaseExecutableMingwX64")
        os.isMacOsX -> tasks.getByName("linkReleaseExecutableMacosX64")
        else -> throw GradleException("Unknown operating system $os")
    }
    project.tasks.register("linkReleaseExecutableMultiplatform") {
        dependsOn(linkReleaseExecutableTaskProvider)
    }

    // disable building of some binaries to speed up build
    // possible values: `all` - build all binaries, `debug` - build only debug binaries
    val enabledExecutables = if (hasProperty("enabledExecutables")) property("enabledExecutables") as String else null
    if (enabledExecutables != null && enabledExecutables != "all") {
        linkReleaseExecutableTaskProvider.enabled = false
    }
}

application {
    mainClass.set("com.icemachined.MainKt")
}
