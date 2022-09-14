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
    val nativeTargets = listOf(linuxX64(), mingwX64(), macosX64())

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
    tasks.matching { it.name.contains("linuxX64", true) || it.name.contains("MacosX64", true)}.configureEach{
        logger.lifecycle("Disabling task :${project.name}:$name")
        enabled = false
    }
}

configurePublishing()
configureDiktat()
configureDetekt()
