import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service

def call(DeployContext deployContext) {
    node() {
        checkout scm
        def proxyName = "${deployContext.journey}-proxy-${deployContext.env}"
        def confHeader = readFile('nginx/nginx.conf.head')
        def confBody = buildProxyBody(deployContext)
        def confTail = readFile('nginx/nginx.conf.tail')

        echo "create proxy directory"
        sh "mkdir -p ${proxyName}"
        dir(proxyName) {
            writeFile file: 'nginx.conf', text: confHeader + confBody + confTail
            archiveArtifacts 'nginx.conf'
        }
        if (deployContext.target == "bluemix") {
            eagleDeployBluemixProxy(proxyName, deployContext)
        }
    }
}

private def buildProxyBody(DeployContext deployContext) {
    def lbgDomain = deployContext.bluemix.domain
    def proxyBody = """
                location / {
                    add_header Content-Type text/plain;
                    return 200 '${deployContext.journey} ${deployContext.env} proxy';
                }
                 """
    for (Service service : deployContext.services) {
        for (proxyItem in service.proxy) {
            def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
            proxyBody = proxyBody + """

						location ${proxyItem.value} {				
							proxy_pass https://${appName}.${lbgDomain}${proxyItem.key} ;
						}
									
					"""
        }
    }
    return proxyBody
}

return this;