import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service

def call(DeployContext deployContext) {
    node() {
        checkout scm
        def proxyName = "${deployContext.release.journey}-proxy-${deployContext.release.environment}"

        def confHeader = libraryResource "com/lbg/workflow/sandbox/bluemix/nginx.conf.head"
        def confBody = buildProxyBody(deployContext)
        def confTail = libraryResource "com/lbg/workflow/sandbox/bluemix/nginx.conf.tail"

        echo "create proxy directory"
        sh "mkdir -p ${proxyName}"
        dir(proxyName) {
            writeFile file: 'nginx.conf', text: confHeader + confBody + confTail
            archiveArtifacts 'nginx.conf'
        }
        eagleDeployBluemixProxy(proxyName, deployContext)
    }
}

private def buildProxyBody(DeployContext deployContext) {
    def lbgDomain = deployContext.platforms.bluemix.domain
    def proxyBody = """
                location / {
                    add_header Content-Type text/plain;
                    return 200 '${deployContext.release.journey} ${deployContext.release.environment} proxy';
                }
                 """

    def proxyPassToken = deployContext?.platforms?.proxy?.proxy_pass_token ?: "API_CONTEXT_ROOT"
    for (Service service : deployContext.services) {
            if (service.tokens[proxyPassToken]) {
                def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
                proxyBody = proxyBody + """

                    location /${service.tokens[proxyPassToken]} {				
                        proxy_pass https://${appName.replace(".", "")}.${lbgDomain}/${service.tokens[proxyPassToken]} ;
                    }
                                
                """
            }

    }
    return proxyBody
}

return this;