import com.lbg.workflow.sandbox.deploy.UtilsUCD


def call(String token, String name) {
    def versionsChoice = ''
    node('lbg_slave') {
        def ucdTokenKey = 'UC_TOKEN_MCA'
        withCredentials([string(credentialsId: ucdTokenKey, variable: 'ucdToken')]) {
            versionsChoice = UtilsUCD.ucdMCAComponentVersion(token, name)
        }
    }
    return versionsChoice
}

return this;
