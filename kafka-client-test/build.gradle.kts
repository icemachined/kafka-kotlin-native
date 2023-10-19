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
        hostOs == "Mac OS X" -> macosArm64()
        hostOs == "Linux" -> linuxX64()
        isMingwX64 -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    configure(listOf(nativeTarget)) {
        binaries {
            executable {
                entryPoint = "com.icemachined.main"
                if (isMingwX64) {
                    val execPath = System.getenv("PATH") + System.getProperty("path.separator") +
                            "${rootProject.projectDir}/packages/librdkafka.redist.2.2.0/runtimes/win-x64/native"
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
    val linkReleaseExecutableTaskProvider = when {
        os.isLinux -> tasks.getByName("linkReleaseExecutableLinuxX64")
        os.isWindows -> tasks.getByName("linkReleaseExecutableMingwX64")
        os.isMacOsX -> tasks.getByName("linkReleaseExecutableMacosArm64")
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

configureDiktat()
configureDetekt()

application {
    mainClass.set("com.icemachined.MainKt")
}
