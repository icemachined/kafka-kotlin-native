import com.icemachined.buildutils.*

configureVersioning()

allprojects {
    configureDiktat()
    configureDetekt()
}

createDetektTask()
installGitHooks()
configurePublishing()
