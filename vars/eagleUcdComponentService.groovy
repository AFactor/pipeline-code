import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext){
    checkAndCreate(deployContext, false)
}

def call(deployContext, String action) {
    if (action == "create") {
        checkAndCreate(deployContext, true)
    } else {
        print "Action ${action} not recognized, running default check..."
        checkAndCreate(deployContext, false)
    }
}

def checkAndCreate(deployContext, boolean create){
    // Ensure UDClient is present
    if (!fileExists('./udclient/udclient'))
        new UtilsUCD().install_by_url(deployContext.platforms.ucd.ucd_url)

    def environment = deployContext.release.environment
    def applicationName = deployContext.platforms.ucd.app_name
    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def utils = new UtilsUCD()
    def FAILURE = false
    def components = [] as Set


    stage('Read tracker config'){
        for (Service service : deployContext.services) {
            componentName = service.platforms.ucd.component_name?: ""
            if (componentName == "") {
                error("Missing UCD or empty component name, service: ${service.name}")
            }
            if (components.contains(componentName)) {
                error("Component ${componentName} specified again in ${service.name}")
            }
            components += componentName
        }
        echo "Read component list for ${applicationName}, environment ${environment}: ${components}"
    }

    stage('Check app components'){
        def appComponents = [] as Set
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            utils.getComponentsInApplication(ucdUrl, ucdToken, applicationName).each { c ->
                appComponents += c.name
            }
        }
        echo "Read components present in ${applicationName}: ${appComponents}"

        def appSurplus = [] as Set
        appSurplus.addAll(appComponents)
        appSurplus.removeAll(components)
        if (!appSurplus.isEmpty()) {
            echo "Components present in ${applicationName} but not release tracker: ${appSurplus}"
            echo "Note that this is OK in case you have separate release trackers for different parts of app, e.g. STORM and SPARK."
        } else {
            echo "No extra components in app that are not in release tracker."
        }

        def compToAdd = [] as Set
        compToAdd.addAll(components)
        compToAdd.removeAll(appComponents)
        if (!compToAdd.isEmpty()) {
            echo "Components missing from ${applicationName}: ${compToAdd}"
            FAILURE = true
            if (create) {
                echo "Adding missing components..."
                withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
                    compToAdd.each { c ->
                        utils.addComponentToApplication(ucdUrl, ucdToken, applicationName, c)
                    }
                }
                FAILURE = false
            }
        } else {
            echo "Application has all the components from release tracker present."
        }
    }

    stage('Check environment resources'){
        // Find out resource path for the specified environment
        def rootPath
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            rootPath = utils.getEnvironmentBaseResources(ucdUrl, ucdToken, applicationName, environment)[0].path
        }

        // Check if components are added into resource tree in the environment
        def agents = [] as Set
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            utils.getResources(ucdUrl, ucdToken, rootPath).each {r ->
                if (r.type == "agent"){ agents += r.name }
            }
        }
        if (agents.isEmpty()){
            error("No agents found in ${environment} environment of ${applicationName}. Please add agents first.")
        }

        // All components are checked in all agents. This is for new OB apps
        // dedicated to specific App with components added to all agents
        agents.each { a ->
            path = "${rootPath}/${a}"
            def agentComponents = [] as Set
            withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
                utils.getResources(ucdUrl, ucdToken, path).each { r ->
                    agentComponents += r.name
                }
            }

            def agentSurplus = [] as Set
            agentSurplus.addAll(agentComponents)
            agentSurplus.removeAll(components)
            if (!agentSurplus.isEmpty()) {
                echo "Components present in ${path} but not release tracker: ${agentSurplus}"
                echo "Note that this is OK in case you have separate release trackers for different parts of app, e.g. STORM and SPARK."
            } else {
                echo "Agent ${a} has no extra components that are not in tracker."
            }

            def compToAdd = [] as Set
            compToAdd.addAll(components)
            compToAdd.removeAll(agentComponents)
            if (!compToAdd.isEmpty()) {
                echo "Components missing from ${path}: ${compToAdd}"
                FAILURE = true
                if (create) {
                    echo "Adding missing components..."
                    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
                        compToAdd.each { c ->
                            utils.addComponentToEnv(ucdUrl, ucdToken, path, c)
                        }
                    }
                    FAILURE = false
                }
            } else {
                echo "Agent ${a} has all the components from tracker present."
            }
        } //agents.each
    }

    stage('Report'){
        if (FAILURE){
            error("There were one or more errors when checking component presence in the app. Check job logs.")
        } else {
            echo "DONE. UCD configurations seems OK."
        }
    }
}

return this
