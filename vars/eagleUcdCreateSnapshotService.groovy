import com.lbg.workflow.sandbox.deploy.ServiceWrapper
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Creating UCD Snapshot"

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def appName = deployContext.platforms.ucd.app_name
    def snapshotName = "${deployContext.releaseVersion()}.${env.GIT_COMMIT}"
    def utils = new UtilsUCD()

    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->

        def snapshotAlreadyExits = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, snapshotName)
        if (snapshotAlreadyExits) {
            echo("*****************************************************")
            echo("* Snapshot ${snapshotName} already exits, skipping. *")
            echo("*****************************************************")
            return snapshotName
        }

        def response = utils.createSnapshot(ucdUrl, ucdToken, createSnapshotJson(deployContext, appName, snapshotName))
        echo("Create snapshot response: ${response}")

        response = utils.lockSnapshotVersions(ucdUrl, ucdToken, appName, snapshotName)
        echo("Lock snapshot versions response: ${response}")
    }

    snapshotName
}

private createSnapshotJson(deployContext, appName, snapshotName) {

    def result = [:]
    result['name'] = snapshotName
    result['application'] = appName
    result['description'] = "Eagle pipeline snapshot of ${appName}, name: ${snapshotName}"

    def versions = []
    for (def service : deployContext.services) {

        ServiceWrapper wrappedService = new ServiceWrapper(service)

        if (!wrappedService.componentName().trim()) {
            error("Missing UCD component name, service: ${service.name}")
        }

        def version = [:]
        version[wrappedService.componentName()] = wrappedService.componentVersion()
        versions.push(version)
    }

    def commonComponents = deployContext.platforms.ucd.snapshot.common
    def entries = UDClient.mapAsList(commonComponents)
    for (def entry in entries) {
        def componentName = entry.get(0)
        def componentVersion = entry.get(1)

        def version = [:]
        version[componentName] = componentVersion
        versions.push(version)
    }

    result['versions'] = versions

    UDClient.jsonFromMap(result)
}

return this