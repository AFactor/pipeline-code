import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext, sourceSnapshotName) {
    echo("Creating UCD Environment Snapshot")

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def appName = deployContext.platforms.ucd.app_name
    def environment = deployContext.release.environment
    def environmentSnapshotName = environmentSnapshotName(sourceSnapshotName, environment, env.BUILD_NUMBER)
    def utils = new UtilsUCD()

    checkout scm
    def commit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->

        def sourceSnapshotExists = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, sourceSnapshotName)
        if (!sourceSnapshotExists) {
            error("Reference snapshot ${sourceSnapshotName} does not exist, exitting!!!")
        }

        def environmentSnapshotExists = utils.snapshotAlreadyExists(ucdUrl, ucdToken, appName, environmentSnapshotName)
        if (environmentSnapshotExists) {
            error("Environment snapshot ${environmentSnapshotName} already exits, exitting!!!")
        }

        def artifactVersions = utils.getSnapshotVersions(ucdUrl, ucdToken, appName, sourceSnapshotName)
        echo("Artifact versions for snapshot ${sourceSnapshotName}: ${artifactVersions}")

        def response = utils.createSnapshot(ucdUrl, ucdToken, createEnvironmentSnapshotJson(artifactVersions, appName, environmentSnapshotName, commit))
        echo("Create snapshot response: ${response}")

        response = utils.lockSnapshotVersions(ucdUrl, ucdToken, appName, environmentSnapshotName)
        echo("Lock snapshot versions response: ${response}")

        response = utils.lockSnapshotConfiguration(ucdUrl, ucdToken, appName, environmentSnapshotName)
        echo("Lock snapshot configuration response: ${response}")
    }

    gerritHandler.createTag(environmentSnapshotName,
                    "Environment Snapshot: ${environmentSnapshotName}, " +
                    "Artifact Snapshot: ${sourceSnapshotName}, " +
                    "User: ${new com.lbg.workflow.global.GlobalUtils().getBuildUser()}")

    printSummary(environmentSnapshotName)
    environmentSnapshotName
}

private createEnvironmentSnapshotJson(artifactVersions, appName, snapshotName, commit) {

    def result = [:]
    result['name'] = snapshotName
    result['application'] = appName
    result['description'] = "Eagle pipeline environment snapshot of ${appName}, name: ${snapshotName}, git_sha: ${commit}"

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

private environmentSnapshotName(sourceSnapshotName, environment, buildNumber) {
    def sourceSnapshotNameWithoutBuildNumber = sourceSnapshotName.substring(0, sourceSnapshotName.lastIndexOf("."))
    "${environment}.${sourceSnapshotNameWithoutBuildNumber}.${buildNumber}"
}

private printSummary(snapshotName) {
    echo """
|=======================================================================|
|***********************************************************************|
|                                                                       |
| ENVIRONMENT SNAPSHOT ${snapshotName} IS NOW CREATED                   |
|                                                                       |
|***********************************************************************|
|=======================================================================|
"""
}

return this