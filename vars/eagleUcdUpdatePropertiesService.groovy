import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Updating UCD Properties"
    for (Service service : deployContext.services) {
        updateUcdComponentEnvironmentProperties(service, deployContext)
    }
}

def call(service, deployContext) {
    updateUcdComponentEnvironmentProperties(service, deployContext)
}

private updateUcdComponentEnvironmentProperties(service, deployContext) {

    if (!service.deploy) {
        echo "Skipping service: ${service.name}"
        return
    }

    if (!service.platforms.ucd.component_name.trim()) {
        error("Missing UCD component name, service: ${service.name}")
    }

    def environment = deployContext.release.environment
    def applicationName = deployContext.platforms.ucd.app_name
    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def componentName = service.platforms.ucd.component_name
    def expectedPropertiesMap = service.tokens
    def utils = new UtilsUCD()

    echo("Checking ${componentName} ${environment} properties")

    def actualPropertiesMap = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getComponentEnvironmentProperties(ucdUrl, ucdToken, applicationName, componentName, environment)
    }

    if (actualPropertiesMap.keySet() != expectedPropertiesMap.keySet()) {
        echo("Actual keys: ${UDClient.sortedSet(actualPropertiesMap.keySet())}")
        echo("Expected keys: ${UDClient.sortedSet(expectedPropertiesMap.keySet())}")
        error("Component ${componentName} environment ${environment} property keys mismatch detected, failing the build!!!")
    }

    def entries = UDClient.mapAsList(actualPropertiesMap)
    for (def entry in entries) {
        def propertyName = entry.get(0)
        def actualPropertyValue = entry.get(1).value
        def isPropertySecure = entry.get(1).secure
        def expectedPropertyValue = expectedPropertiesMap[propertyName]

        if (isPropertySecure || actualPropertyValue == expectedPropertyValue)
            continue

        echo("Setting property: ${propertyName}, actual value: ${actualPropertyValue}, expected value: ${expectedPropertyValue}")
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            utils.setComponentEnvironmentProperty(ucdUrl, ucdToken, applicationName, componentName, environment, propertyName, expectedPropertyValue)
        }
    }
}

return this
