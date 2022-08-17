/**
 * Configuration for detekt static analysis
 */

package com.icemachined.buildutils

import com.ullink.NuGetPlugin
import com.ullink.NuGetRestore
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType

/**
 * Configure Nuget for a single project
 */
fun Project.configureNuget() {
    apply<NuGetPlugin>()
    tasks.withType<NuGetRestore>().configureEach {
        packagesDirectory = rootProject.file("packages")
        setPackagesConfigFile(rootProject.file("packages.config").relativeTo(project.projectDir).path)
    }
}
