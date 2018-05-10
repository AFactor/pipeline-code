import com.lbg.workflow.sandbox.deploy.UtilsBluemix
import com.lbg.workflow.global.GlobalUtils

def call(proxyName, deployContext) {
    if(needsDeployment(proxyName, deployContext)) {
        def utils = new UtilsBluemix()
        def bluemixEnvs = utils.buildBluemixEnv(deployContext.platforms.bluemix)
        bluemixEnvs["APP"] = proxyName
        bluemixEnvs["deployable"] = proxyName
        bluemixEnvs["TOKENS_DIGEST"] = getTokensDigest(proxyName, deployContext)

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
    } else {
		echo "service ${deployContext.release.journey}-${proxyName}-${deployContext.release.environment} already exists"
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
            cf set-env \${APP} TOKENS_DIGEST \${TOKENS_DIGEST}
        }
    """
}

def needsDeployment(proxyName, deployContext) {
    try {
		def utils = new UtilsBluemix()
        def bluemixEnvs = utils.buildBluemixEnv(deployContext.platforms.bluemix)
		String tokensDigest = "TOKENS_DIGEST: ${getTokensDigest(proxyName, deployContext)}"
		withCredentials([
				usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
						passwordVariable: 'BM_PASS',
						usernameVariable: 'BM_USER')
		]) {
			withEnv(utils.toWithEnv(bluemixEnvs)) {
				String result = sh(script: "export HTTP_PROXY=\"http://10.113.140.187:3128\";export HTTPS_PROXY=\"http://10.113.140.187:3128\";export http_proxy=\"http://10.113.140.187:3128\";export https_proxy=\"http://10.113.140.187:3128\";export no_proxy=localhost,127.0.0.1,sandbox.local,lbg.eu-gb.mybluemix.net,lbg.eu-gb.bluemix.net; cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV 1>/dev/null && cf app ${proxyName} && cf env ${proxyName}", returnStdout: true, returnStatus: false).trim()
                
				if ((result ==~ /(?s)(.*)(0\\/[1-9])(.*)/) ||
						result.contains("CRASHED") ||
						result.contains("starting") ||
					result.contains("flapping") ||
					!(result ==~ /(?s)(.*)($tokensDigest)(.*)/)) {
                        
					echo "app check - deploy app ${proxyName}"
					return true
				}
				echo "app check - skip deployment - token digest match ${tokensDigest}"
				return false
			}
        }
    } catch (error) {
        echo "app check failed - deploy app ${proxyName}"
        return true
    }
}

def getTokensDigest(proxyName, deployContext) {
	GlobalUtils utils = new GlobalUtils()
    def config = readFile("${proxyName}/nginx.conf")
	def digest = utils.generateDigest("SHA-512", config)
	echo "digest: ${digest}"
	return digest
}