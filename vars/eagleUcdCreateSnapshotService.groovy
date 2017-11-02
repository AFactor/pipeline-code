import com.lbg.workflow.sandbox.deploy.ServiceWrapper
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Creating UCD Snapshot"

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def appName = deployContext.platforms.ucd.app_name
    def snapshotName = "${deployContext.releaseVersion()}.${env.BUILD_TIMESTAMP}"
    def shouldLockSnapshot = deployContext.platforms.ucd.lock_snapshot

    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        def utils = new UtilsUCD()

        def snapshotAlreadyExits = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, snapshotName)

        if (snapshotAlreadyExits) {
            echo("*****************************************************")
            echo("* Snapshot ${snapshotName} already exits, skipping. *")
            echo("*****************************************************")
            return
        }

        def response = utils.createSnapshot(ucdUrl, ucdToken, createSnapshotJson(deployContext))
        echo("Create snapshot response: ${response}")

        if (shouldLockSnapshot) {
            response = utils.lockSnapshotVersions(ucdUrl, ucdToken, appName, snapshotName)
            echo("Lock snapshot versions response: ${response}")
        }
    }
}

private createSnapshotJson(deployContext) {

    def appName = deployContext.platforms.ucd.app_name
    def snapshotName = "${deployContext.releaseVersion()}.${env.BUILD_TIMESTAMP}"

    def result = [:]
    result['name'] = snapshotName
    result['application'] = appName
    result['description'] = "Eagle pipeline snapshot of ${appName}, name: ${snapshotName}"

    def versions = []
    for (def service : deployContext.services) {
        if(!service.deploy) {
            echo "Skipping service from spanshot: ${service.name}"
            continue
        }

        ServiceWrapper wrappedService = new ServiceWrapper(service)

        if (!wrappedService.componentName().trim()) {
            error("Missing UCD component name, service: ${service.name}")
        }

        def version = [:]
        version[wrappedService.componentName()] = wrappedService.componentVersion()
        versions.push(version)
    }
    result['versions'] = versions

    UDClient.jsonFromMap(result)
}

return this