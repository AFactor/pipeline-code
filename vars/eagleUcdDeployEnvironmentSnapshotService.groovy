import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(deployContext) {
    def snapshotName
    def outcome
    def snapshotFilter = "${deployContext.release.environment}.OB-R-"
    def snapshots = eagleUcdGetSnapshotsService(deployContext, snapshotFilter, true, true)
    timeout(time: 300, unit: 'SECONDS') {
        outcome = input id: 'environmentSnapshot',
                message: 'Select environment snapshot & confirm deployment',
                ok: 'Deploy',
                parameters: [
                        [
                                $class     : 'ChoiceParameterDefinition', choices: "${snapshots.join("\n")}",
                                name       : 'Environment Snapshot',
                                description: ''
                        ],
                        [
                                $class      : 'BooleanParameterDefinition',
                                defaultValue: false,
                                name        : 'Confirm Deployment',
                                description : ''
                        ]
                ]
    }

    snapshotName = outcome.get('Environment Snapshot')
    echo "Confirm Deployment: ${outcome.get('Confirm Deployment')}"
    if (snapshotName && outcome.get('Confirm Deployment')) {
        call(deployContext, snapshotName)
    }
}

def call(deployContext, snapshotName) {

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def applicationName = deployContext.platforms.ucd.app_name
    def environment = deployContext.release.environment
    UtilsUCD utils = new UtilsUCD()

    echo "Deploying UCD Environment Snapshot ${snapshotName} to ${environment} environment"

    utils.install_by_url(ucdUrl)

    // snapshot validation
    if (!snapshotName.startsWith(environment)) {
        error("Snapshot name ${snapshotName} for ${environment} environment should start with '${environment}', exitting!!!")
    }

    def snapshotAlreadyExists = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.snapshotAlreadyExists(ucdUrl, ucdToken, applicationName, snapshotName)
    }

    if (!snapshotAlreadyExists) {
        error("Snapshot ${snapshotName} does not exist, exitting!!!")
    }

    def snapshot = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.getSnapshot(ucdUrl, ucdToken, applicationName, snapshotName)
    }

    if (!snapshot.versionsLocked) {
        error("Snapshot ${snapshotName} is not version locked, exitting!!!")
    }

    if (!snapshot.configLocked) {
        error("Snapshot ${snapshotName} is not config locked, exitting!!!")
    }


    // reference snapshot validation
    def referenceSnapshot = snapshotName.substring(environment.length() + 1)

    def referenceSnapshotAlreadyExists = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        utils.snapshotAlreadyExists(ucdUrl, ucdToken, applicationName, referenceSnapshot)
    }

    if (!referenceSnapshotAlreadyExists) {
        error("Reference snapshot ${referenceSnapshot} does not exist, exitting!!!")
    }


    eagleUcdDeploySnapshotService(deployContext, snapshotName)
}

return this
