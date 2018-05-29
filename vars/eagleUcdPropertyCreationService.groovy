import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Creating UCD Properties"

    // make sure ucd client is installed before starting parallel tasks
    if (!fileExists('./udclient/udclient'))
        new UtilsUCD().install_by_url(deployContext.platforms.ucd.ucd_url)

    def parallelJobs = [failFast: false]
    for (Service service : deployContext.services) {
        def localService = service
        parallelJobs[localService.name] = { createUcdComponentEnvironmentProperties(localService, deployContext) }
    }

    try {
        parallel(parallelJobs)
    } catch (error) {
        echo "Error running ucd property update in parallel: ${error.message}"
        echo "Continuing despite failures."
        throw error
    }
}

def call(service, deployContext) {
    createUcdComponentEnvironmentProperties(service, deployContext)
}

private createUcdComponentEnvironmentProperties(service, deployContext) {

    if (!service.platforms.ucd.component_name.trim()) {
        error("Missing UCD component name, service: ${service.name}")
    }

    def environment = deployContext.release.environment
    def applicationName = deployContext.platforms.ucd.app_name
    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def componentName = service.platforms.ucd.component_name
    def utils = new UtilsUCD()

    echo("Checking ${componentName} ${environment} properties")

    def ucdPropertiesMap = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getComponentEnvironmentProperties(ucdUrl, ucdToken, applicationName, componentName, environment)
    }

    service.tokens.each {k,v ->
        if (! ucdPropertiesMap.containsKey(k)){
            echo("Creating property: ${k}, default value: ${v}")
            withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
                utils.createComponentEnvironmentProperty("${ucdUrl}", "${ucdToken}", "${componentName}", "${k}", "${v}")
            }
        }
    }
}

return this
