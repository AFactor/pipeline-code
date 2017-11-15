import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(deployContext) {

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def applicationName = deployContext.platforms.ucd.app_name
    def snapshotName = deployContext.release.deploySnapshot
    def environment = deployContext.release.environment
    UtilsUCD utils = new UtilsUCD()

    echo "Promoting UCD Snapshot ${snapshotName} to ${environment} environment"

    utils.install_by_url(ucdUrl)

    def snapshotAlreadyExists = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.snapshotAlreadyExists(ucdUrl, ucdToken, applicationName, snapshotName)
    }

    if (!snapshotAlreadyExists) {
        error("Snapshot ${snapshotName} of app '${applicationName}' does not exist, exitting!!!")
    }

    eagleUcdUpdatePropertiesService(deployContext)

    eagleUcdDeploySnapshotService(deployContext, snapshotName)
}


return this
