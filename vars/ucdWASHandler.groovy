import com.lbg.workflow.sandbox.deploy.UtilsUCD


def call(String name) {
    def versionsChoice = ''
    node('lbg_slave') {
        def ucdTokenKey = 'UC_TOKEN_MCA'
        withCredentials([string(credentialsId: ucdTokenKey, variable: 'ucdToken')]) {
            withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
                def utils = new UtilsUCD()
                versionsChoice = utils.ucdComponentVersionGather(ucdToken, name)
            }
        }
    }
    return versionsChoice
}

return this;
