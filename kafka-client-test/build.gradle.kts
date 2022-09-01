import com.icemachined.buildutils.configureDetekt
import com.icemachined.buildutils.configureDiktat

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
            }
        }
    }
    sourceSets {
        val commonMain by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(projects.kafkaClient)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        nativeTarget.let {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
    }
}

configureDiktat()
configureDetekt()

application {
    mainClass.set("com.icemachined.MainKt")
}
