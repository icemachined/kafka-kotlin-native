import com.icemachined.buildutils.*

configureVersioning()

allprojects {
    repositories {
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()

    tasks.withType<org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase> {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}

createDetektTask()
installGitHooks()
configurePublishing()
