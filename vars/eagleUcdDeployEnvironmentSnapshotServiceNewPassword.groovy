import com.lbg.workflow.sandbox.deploy.UtilsUCDNewPassword

def call(deployContext) {
    def snapshotName
    def userName
    def passWord
    def outcome
    def snapshotFilter = deployContext?.release?.deploySnapshotFilter ?: deployContext.release.environment
    def snapshots = eagleUcdGetSnapshotsService(deployContext, snapshotFilter, true, true)
    timeout(time: 300, unit: 'SECONDS') {
        outcome = input id: 'environmentSnapshot',
                message: 'Select environment snapshot & confirm deployment',
                ok: 'Deploy',
                parameters: [
                        [
                                $class     : 'hudson.model.ChoiceParameterDefinition', choices: "${snapshots.join("\n")}",
                                name       : 'Environment Snapshot',
                                description: ''
                        ],
                        [
                                $class      : 'hudson.model.BooleanParameterDefinition',
                                defaultValue: false,
                                name        : 'Confirm Deployment',
                                description : ''
                        ],
                        [
                                $class      : 'hudson.model.StringParameterDefinition',
                                defaultValue: '',
                                name        : 'Username',
                                description : ''
                        ],
                        [
                                $class      : 'hudson.model.PasswordParameterDefinition',
                                defaultValue: '',
                                name        : 'Password',
                                description : ''
                        ]

                ]
    }

    snapshotName = outcome.get('Environment Snapshot')
    userName = outcome.get('Username')
    passWord = outcome.get('Password')

    echo "Confirm Deployment: ${outcome.get('Confirm Deployment')}"
    if (snapshotName && outcome.get('Confirm Deployment')) {
        call(deployContext, snapshotName, userName, passWord)
    }
}

def call(deployContext, snapshotName, userName, passWord) {

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    //def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def ucduserName = userName
    def ucdPassword = passWord
    def applicationName = deployContext.platforms.ucd.app_name
    def environment = deployContext.release.environment
    UtilsUCDNewPassword utils = new UtilsUCDNewPassword()

    echo "Deploying UCD Environment Snapshot ${snapshotName} to ${environment} environment"

    utils.install_by_url(ucdUrl)

    // snapshot validation
    if (!snapshotName.startsWith(environment)) {
        error("Snapshot name ${snapshotName} for ${environment} environment should start with '${environment}', exitting!!!")
    }

    def snapshotAlreadyExists = utils.snapshotAlreadyExists(ucdUrl, ucduserName, ucdPassword, applicationName, snapshotName)
    

    if (!snapshotAlreadyExists) {
        error("Snapshot ${snapshotName} does not exist, exitting!!!")
    }

    def snapshot = utils.getSnapshot(ucdUrl, ucduserName, ucdPassword, applicationName, snapshotName)
    

    if (!snapshot.versionsLocked) {
        error("Snapshot ${snapshotName} is not version locked, exitting!!!")
    }

    if (!snapshot.configLocked) {
        error("Snapshot ${snapshotName} is not config locked, exitting!!!")
    }

    eagleUcdDeploySnapshotServiceNewPassword(deployContext, ucduserName, ucdPassword, snapshotName)
}

return this
