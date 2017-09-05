import com.lbg.workflow.sandbox.deploy.UtilsUCD


def call(String name, deployContext) {
    def envsChoice = ''
    node(deployContext.label) {
        withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
            withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
                def utils = new UtilsUCD()
                envsChoice = utils.ucdApplicationEnvironments(ucdToken, name)
            }
        }
    }
    return envsChoice
}

return this;
