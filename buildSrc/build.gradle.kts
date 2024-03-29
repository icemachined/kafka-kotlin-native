plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // this hack prevents the following bug: https://github.com/gradle/gradle/issues/9770
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.reckon.gradle.plugin)
    implementation(libs.nuget.gradle.plugin)
    implementation(libs.publish.gradle.plugin)
    implementation(libs.kotlin.plugin.serialization)

}
