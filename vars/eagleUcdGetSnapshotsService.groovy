import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(deployContext, prefix, versionsLocked, configLock) {

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def applicationName = deployContext.platforms.ucd.app_name
    def environment = deployContext.release.environment
    UtilsUCD utils = new UtilsUCD()

    echo "Get UCD Snapshots for ${applicationName} to ${environment} environment"

    utils.install_by_url(ucdUrl)

    def snapshotsList = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getSnapshotsInApplication(ucdUrl, ucdToken, applicationName, 500)
    }
    def snapshots = []
    for (def s  : snapshotsList) {
        if (s.name.startsWith(prefix) && s.configLocked == configLock && s.versionsLocked  == versionsLocked) {
            snapshots.add(s.name)
        }
    }
    echo "snapshots:$snapshots"
    return snapshots
}


return this
