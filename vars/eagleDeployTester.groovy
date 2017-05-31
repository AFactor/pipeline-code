import com.lbg.workflow.sandbox.deploy.Service

def call(deployContext) {
    node() {
        if (deployContext.target == "apiconnect") {
            echo "TODO: test apiconnect published catalogue"
        }
        else if (deployContext.target == "bluemix") {
            def lbgDomain = deployContext.bluemix.domain
            for (Service service : deployContext.services) {
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
}

return this;