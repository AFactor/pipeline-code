import com.lbg.workflow.sandbox.deploy.Service

def call(deployContext) {
    node() {
        def lbgDomain = deployContext.bluemix.domain
        for (Service service: deployContext.services){
            if (service.deploy == true) {
                try {
                    pingTester("${deployContext.journey}-${service.name}-${deployContext.env}.${lbgDomain}")
                } catch (error) {
                    currentBuild.result = 'FAILURE'
                    echo error.message
                    throw error
                }
            }
        }

        if (deployContext.proxy.deploy == true) {
            try {
                pingTester("${deployContext.journey}-proxy-${deployContext.env}.${lbgDomain}")
            } catch (error) {
                currentBuild.result = 'FAILURE'
                echo error.message
                throw error
            }
        }
    }
}

return this;