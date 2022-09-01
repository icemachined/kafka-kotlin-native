rootProject.name = "kafka-kotlin-native"
include("kafka-client")
include("kafka-client-test")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")