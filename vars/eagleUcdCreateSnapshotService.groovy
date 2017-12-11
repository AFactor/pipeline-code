import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.ServiceWrapper
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Creating UCD Reference Snapshot"

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def appName = deployContext.platforms.ucd.app_name
    def snapshotName = "${deployContext.releaseVersion()}.${env.BUILD_NUMBER}"
    def utils = new UtilsUCD()

    checkout scm
    def commit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->

        def snapshotAlreadyExits = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, snapshotName)
        if (snapshotAlreadyExits) {
            error("Reference snapshot ${snapshotName} already exists, exitting!!!")
        }

        def response = utils.createSnapshot(ucdUrl, ucdToken, createSnapshotJson(deployContext, appName, snapshotName, commit))
        echo("Create snapshot response: ${response}")

        response = utils.lockSnapshotVersions(ucdUrl, ucdToken, appName, snapshotName)
        echo("Lock snapshot versions response: ${response}")
    }

    snapshotName
}

private createSnapshotJson(deployContext, appName, snapshotName, commit) {

    def result = [:]
    result['name'] = snapshotName
    result['application'] = appName
    result['description'] = "Eagle pipeline reference snapshot of ${appName}, name: ${snapshotName}, git_sha: ${commit}"

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
    def entries = GlobalUtils.mapAsList(commonComponents)
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