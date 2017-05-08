import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service

def call(String configuration) {

    DeployContext deployContext

    stage('Initialize') {
        node() {
            deleteDir()
            checkout scm
            deployContext = new DeployContext(readFile(configuration))
            echo "Deploy Context " + deployContext.toString()
        }
    }
    milestone(label: 'Initialized')

    lock(inversePrecedence: true, quantity: 1, resource: "j2-${deployContext.journey}-deploy") {

        stage('Deploy Services') {
            def deployments = [:]
            for (Object serviceObject : deployContext.services) {
                Service service = serviceObject
                if (service.deploy) {
                    echo "service $service.name"
                    deployments["${service.name}: ${artifactTag(service)}"] = {
                        eagleDeployService(service, deployContext)
                    }
                } else {
                    echo "skipping service $service.name"
                }
            }
            try {
                echo "parallel deployments $deployments"
                parallel deployments
            } catch (error) {
                echo "Deploy Service Failure $error.message"
                throw error
            } finally {
            }
        }
        milestone(label: 'Deployed Services')

        if (null != deployContext?.proxy?.deploy && deployContext.proxy.deploy == true) {
            stage('Deploy Proxy') {
                try {
                    eagleDeployProxy(deployContext)
                } catch (error) {
                    echo "Deploy Proxy Failure  $error.message"
                    throw error
                } finally {
                }
            }
            milestone(label: 'Deployed Proxy')
        } else {
            echo "skipping proxy"
        }

        stage('Test') {
            try {
                eagleDeployTester(deployContext)
            } catch (error) {
                echo "Test Stage Failure $error.message"
                throw error
            } finally {
            }
        }
        milestone(label: 'Test Services and Proxy')

    } //End Lock

    milestone(label: 'Cleanup')
    stage('End') {
        echo "Finished"
    }

}

private def artifactTag(service) {
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    return artifactName.split("artifact-")[1]
}

return this;