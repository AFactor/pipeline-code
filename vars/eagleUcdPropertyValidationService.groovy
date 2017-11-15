import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Comparing UCD Properties"
    def isSuccess = true
    for (def service : deployContext.services) {
        def serviceSuccess = compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext)
        if (!serviceSuccess) {
            isSuccess = false
        }
    }

    if (!isSuccess) {
        error("Dry-run failure, please check log for details!!!")
    }
}

def call(service, deployContext) {
    compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext)
}

private compareUcdEnvironmentPropertiesToServiceConfig(service, deployContext) {

    def isSuccess = true

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
        echo("UCD keys: ${UDClient.sortedSet(actualPropertiesMap.keySet())}")
        echo("Config keys: ${UDClient.sortedSet(expectedPropertiesMap.keySet())}")
        echo("Component ${componentName} environment ${environment} property keys mismatch detected")
        isSuccess = false
    }

    def entries = UDClient.mapAsList(actualPropertiesMap)
    for (def entry in entries) {
        def propertyName = entry.get(0)
        def actualPropertyValue = entry.get(1).value
        def isPropertySecure = entry.get(1).secure
        def expectedPropertyValue = expectedPropertiesMap[propertyName]

        if (isPropertySecure || actualPropertyValue == expectedPropertyValue)
            continue

        echo("Component ${componentName} property: ${propertyName} mismatch, ucd value: ${actualPropertyValue}, config value: ${expectedPropertyValue}")
    }

    if (!isSuccess) {
        // display actual service tokens json for easy copy/paste
        def tokens = [:]
        for (def entry in entries) {
            tokens[entry.get(0)] = entry.get(1).value
        }
        echo("Easy copy/paste json:\n${UDClient.jsonFromMap(["name": service.name, "tokens": tokens])},")
    }

    return isSuccess
}

return this
