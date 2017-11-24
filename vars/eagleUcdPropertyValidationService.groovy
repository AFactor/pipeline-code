import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Comparing UCD Properties"

    // make sure ucd client is installed before starting parallel tasks
    if (!fileExists('./udclient/udclient'))
        new UtilsUCD().install_by_url(deployContext.platforms.ucd.ucd_url)

    def statuses = []
    def parallelJobs = [failFast: false]
    for (def service : deployContext.services) {
        def localService = service
        def status = [:]
        statuses.push(status)
        parallelJobs[localService.name] = {
            def props = collateServiceProperties(localService, deployContext)
            status.putAll(props)
        }
    }

    try {
        parallel(parallelJobs)
    } catch (error) {
        echo "Error running ucd property validation in parallel: ${error.message}"
        echo "Continuing despite failures."
        throw error
    }

    summurize(statuses)
}

private collateServiceProperties(service, deployContext) {

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

    def ucdPropertiesMap = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getComponentEnvironmentProperties(ucdUrl, ucdToken, applicationName, componentName, environment)
    }

    [service_name: service.name, ucd_properties: ucdPropertiesMap, config_properties: configPropertiesMap]
}

private summurize(statuses) {
    def isSuccess = true

    for(def status in statuses ) {
        def serviceName = status.service_name
        def ucdPropertiesMap = status.ucd_properties
        def configPropertiesMap = status.config_properties

        def ucdSurplus = [] as Set
        def configSurplus = [] as Set

        // keys
        if (ucdPropertiesMap.keySet() != configPropertiesMap.keySet()) {
            isSuccess = false

            ucdSurplus.addAll(ucdPropertiesMap.keySet())
            configSurplus.addAll(configPropertiesMap.keySet())

            // removeAll is a mutating method !!!
            ucdSurplus.removeAll(configPropertiesMap.keySet())
            configSurplus.removeAll(ucdPropertiesMap.keySet())
        }

        // values
        def valueDiffs = []
        def entries = GlobalUtils.mapAsList(ucdPropertiesMap)
        for (def entry in entries) {
            def propertyName = entry.get(0)
            def ucdPropertyValue = entry.get(1).value
            def isPropertySecure = entry.get(1).secure
            def configPropertyValue = configPropertiesMap[propertyName]

            if (isPropertySecure || ucdPropertyValue == configPropertyValue)
                continue

            valueDiffs.push([property_name: propertyName, config_value: configPropertyValue, ucd_value: ucdPropertyValue])
        }

        // print summary
        echo(statusMessage(serviceName, ucdSurplus, configSurplus, tokensValueDiff(valueDiffs)))
    }

    if (!isSuccess) {
        error("Property validation failure, failing the build!!!")
    }
}

private statusMessage(serviceName, ucdSurplus, configSurplus, tokensValueDiff) {
"""
=======================================================================
SERVICE: ${serviceName}
UCD_KEY_SURPLUS: ${ucdSurplus}
CONFIG_KEY_SURPLUS: ${configSurplus}
TOKEN_VALUES_DIFF:${tokensValueDiff}
=======================================================================
"""
}

private tokensValueDiff(valueDiffs) {
    def tokens = []
    for(def valueDiff in valueDiffs) {
        def propertyName = valueDiff.property_name
        def configValue = valueDiff.config_value
        def ucdValue = valueDiff.ucd_value

        tokens.push("${propertyName}\n\t\tconf: \"${configValue}\"\n\t\tucde: \"${ucdValue}\"")
    }
    tokens.isEmpty() ? "" : ("\n\t" + tokens.join("\n\t"))
}

return this
