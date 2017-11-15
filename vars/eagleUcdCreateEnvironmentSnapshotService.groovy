import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext, sourceSnapshotName) {
    echo("Creating UCD Rollback Snapshot")

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def appName = deployContext.platforms.ucd.app_name
    def environment = deployContext.release.environment
    def rollbackSnapshotName = "${environment}.${sourceSnapshotName}"
    def utils = new UtilsUCD()

    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->

        def snapshotAlreadyExits = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, rollbackSnapshotName)
        if (snapshotAlreadyExits) {
            echo("*****************************************************")
            echo("* Rollback snapshot ${rollbackSnapshotName} already exits, skipping. *")
            echo("*****************************************************")
            return rollbackSnapshotName
        }

        def artifactVersions = utils.getSnapshotVersions(ucdUrl, ucdToken, appName, sourceSnapshotName)
        echo("Artifact versions for snapshot ${sourceSnapshotName}: ${artifactVersions}")

        def response = utils.createSnapshot(ucdUrl, ucdToken, createRollbackSnapshotJson(artifactVersions, appName, rollbackSnapshotName))
        echo("Create snapshot response: ${response}")

        response = utils.lockSnapshotVersions(ucdUrl, ucdToken, appName, rollbackSnapshotName)
        echo("Lock snapshot versions response: ${response}")

        response = utils.lockSnapshotConfiguration(ucdUrl, ucdToken, appName, rollbackSnapshotName)
        echo("Lock snapshot configuration response: ${response}")
    }

    rollbackSnapshotName
}

private createRollbackSnapshotJson(artifactVersions, appName, snapshotName) {

    def result = [:]
    result['name'] = snapshotName
    result['application'] = appName
    result['description'] = "Eagle pipeline rollback snapshot of ${appName}, name: ${snapshotName}"

    def versions = []
    for (def artifactVersion : artifactVersions) {
        def componentName = artifactVersion.get(0)
        def componentVersion = artifactVersion.get(1)

        def version = [:]
        version[componentName] = componentVersion
        versions.push(version)
    }

    result['versions'] = versions

    UDClient.jsonFromMap(result)
}

return this