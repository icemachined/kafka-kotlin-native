import com.icemachined.buildutils.configureDetekt
import com.icemachined.buildutils.configureDiktat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> listOf(macosX64(), macosArm64())
        hostOs == "Linux" -> listOf(linuxX64())
        isMingwX64 -> listOf(mingwX64())
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    configure(nativeTarget) {
        binaries {
            executable {
                entryPoint = "com.icemachined.main"
                if (isMingwX64) {
                    val execPath = System.getenv("PATH") + System.getProperty("path.separator") +
                            "${rootProject.projectDir}/packages/librdkafka.redist.1.9.2/runtimes/win-x64/native"
                    runTask?.setEnvironment("PATH" to execPath)
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kafkaClient)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        nativeTarget.let {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
    }
    linkProperExecutable(getCurrentOperatingSystem())
}

/**
 * @param os
 * @throws GradleException
 */
fun linkProperExecutable(os: org.gradle.nativeplatform.platform.internal.DefaultOperatingSystem) {
    val linkReleaseExecutableTaskProviders = when {
        os.isLinux -> listOf(tasks.getByName("linkReleaseExecutableLinuxX64"))
        os.isWindows -> listOf(tasks.getByName("linkReleaseExecutableMingwX64"))
        os.isMacOsX -> listOf(tasks.getByName("linkReleaseExecutableMacosArm64"))
        else -> throw GradleException("Unknown operating system $os")
    }
    project.tasks.register("linkReleaseExecutableMultiplatform") {
        linkReleaseExecutableTaskProviders.forEach {linkReleaseExecutableTaskProvider ->
            dependsOn(linkReleaseExecutableTaskProvider)
        }
    }

    // disable building of some binaries to speed up build
    // possible values: `all` - build all binaries, `debug` - build only debug binaries
    val enabledExecutables = if (hasProperty("enabledExecutables")) property("enabledExecutables") as String else null
    if (enabledExecutables != null && enabledExecutables != "all") {
        linkReleaseExecutableTaskProviders.forEach { linkReleaseExecutableTaskProvider ->
            linkReleaseExecutableTaskProvider.enabled = false
        }
    }
}

configureDiktat()
configureDetekt()

application {
    mainClass.set("com.icemachined.MainKt")
}
