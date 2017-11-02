import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(proxyName, deployContext) {
    def utils = new UtilsBluemix()
    def bluemixEnvs = utils.buildBluemixEnv(deployContext.platforms.bluemix)
    bluemixEnvs["APP"] = proxyName
    bluemixEnvs["deployable"] = proxyName

    withCredentials([
            usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
                    passwordVariable: 'BM_PASS',
                    usernameVariable: 'BM_USER')
    ]) {
        withEnv(utils.toWithEnv(bluemixEnvs)) {
            try {
                sh "mkdir -p pipelines/scripts/"
                writeFile file: "pipelines/scripts/deploy.sh", text: deployProxyScript()
                sh 'source pipelines/scripts/deploy.sh; deployProxy'
            } catch (error) {
                echo "Nginx Deployment failed"
                throw error
            } finally {
                step([$class: 'WsCleanup', notFailBuild: true])
            }
        }
    }
}

private String deployProxyScript() {
    return """
        #!/bin/bash
        set -ex

        function deployProxy() {
            cd \$deployable
            cf logout && touch Staticfile
            cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV
            cf delete \${APP} -f -r || echo "Failed to delete old application."
            cf push \${APP} -m 256M -k 256M -t 60 -s cflinuxfs2
        }
    """
}