import com.lbg.workflow.sandbox.deploy.UtilsUCD


def call(String name) {
    def versionsChoice = ''
    node('lbg_slave') {
        def ucdTokenKey = 'UC_TOKEN_MCA'
        withCredentials([string(credentialsId: ucdTokenKey, variable: 'ucdToken')]) {
            def utils = new UtilsUCD()
            versionsChoice = utils.ucdMCAComponentVersion(ucdToken, name)
        }
    }
    return versionsChoice
}

return this;
