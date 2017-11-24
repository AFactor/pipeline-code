import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Updating UCD Properties"

    def parallelJobs = [failFast: false]
    for (Service service : deployContext.services) {
        def localService = service
        parallelJobs[localService.name] = { updateUcdComponentEnvironmentProperties(localService, deployContext) }
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
    updateUcdComponentEnvironmentProperties(service, deployContext)
}

private updateUcdComponentEnvironmentProperties(service, deployContext) {

    if (!service.platforms.ucd.component_name.trim()) {
        error("Missing UCD component name, service: ${service.name}")
    }

    def environment = deployContext.release.environment
    def applicationName = deployContext.platforms.ucd.app_name
    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def componentName = service.platforms.ucd.component_name
    def configPropertiesMap = service.tokens
    def utils = new UtilsUCD()

    echo("Checking ${componentName} ${environment} properties")

    def ucdPropertiesMap = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getComponentEnvironmentProperties(ucdUrl, ucdToken, applicationName, componentName, environment)
    }

    def entries = GlobalUtils.mapAsList(ucdPropertiesMap)
    for (def entry in entries) {
        def propertyName = entry.get(0)
        def ucdPropertyValue = entry.get(1).value
        def isPropertySecure = entry.get(1).secure
        def configPropertyValue = configPropertiesMap[propertyName]

        if (isPropertySecure || ucdPropertyValue == configPropertyValue)
            continue

        echo("Setting property: ${propertyName}, actual value: ${ucdPropertyValue}, expected value: ${configPropertyValue}")
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            utils.setComponentEnvironmentProperty(ucdUrl, ucdToken, applicationName, componentName, environment, propertyName, configPropertyValue)
        }
    }
}

return this
