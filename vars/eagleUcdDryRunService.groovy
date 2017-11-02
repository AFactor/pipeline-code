import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Comparing UCD Properties"
    for (def service : deployContext.services) {
        compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext)
    }
}

def call(service, deployContext) {
    compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext)
}

private compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext) {

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

        error("Component ${componentName} property: ${propertyName} mismatch, actual value: ${actualPropertyValue}, expected value: ${expectedPropertyValue}, failing!!!")
    }
}

return this
