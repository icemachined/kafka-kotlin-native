package com.icemachined.buildutils

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

import com.icemachined.buildutils.configurePublishing
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version

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
        hostOs == "Mac OS X" -> setOf("macosX64", "macosArm64")
        hostOs == "Linux" -> setOf("linuxX64")
        isMingwX64 -> setOf("mingwX64")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val targetNames = nativeTargets.map{ it.name }.filter { !currentTargetName.contains(it) }.toList()
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
