package com.icemachined.buildutils

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    val nativeTargets = listOf(linuxX64(), mingwX64(), macosX64(), macosArm64())

    nativeTargets.forEach() {
        it.apply {
            compilations.getByName("main") {
                cinterops {
                    val librdkafka by creating
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        nativeTargets.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val currentTargetName = when {
        hostOs == "Mac OS X" -> if (DefaultNativePlatform.getCurrentArchitecture().isArm) "macosArm64" else "macosX64"
        hostOs == "Linux" -> "linuxX64"
        isMingwX64 -> "mingwX64"
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val targetNames = nativeTargets.map{ it.name }.filter { !currentTargetName.equals(it) }.toList()
    tasks.matching { task ->
        !(targetNames.find {
            task.name.contains(it, true)
        }.isNullOrEmpty())
    }.configureEach {
        logger.lifecycle("Disabling task :${project.name}:$name")
        enabled = false
    }
}

configurePublishing()
configureDiktat()
configureDetekt()
