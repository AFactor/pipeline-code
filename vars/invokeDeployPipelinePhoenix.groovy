import com.lbg.workflow.sandbox.deploy.phoenix.DeployContext
import com.lbg.workflow.sandbox.deploy.phoenix.Service
import com.lbg.workflow.sandbox.JobStats

def call(String configuration) {

    DeployContext deployContext

    stage('Initialize') {
        node() {
            deleteDir()
            checkout scm
            try {
                deployContext = new DeployContext(readFile(configuration))
                validate(deployContext)
            } catch (error) {
                echo "Invalid job configuration $error.message"
                currentBuild.result = 'FAILURE'
                phoenixNotifyStage().notify(deployContext)
                throw error
            }
            echo "Deploy Context " + deployContext.toString()
        }
    }
    milestone(label: 'Initialized')

    lock(inversePrecedence: true, quantity: 1, resource: "j2-${deployContext.journey}-deploy") {
        testStage = new phoenixTestStage()
        deployStage = new phoenixDeployStage()
        notifyStage = new phoenixNotifyStage()
        switch (deployContext.deployment.type) {
            case 'ucd':
                stage('environment Check') {
                    testStage.envCheck(deployContext)
                }
                milestone(label: 'Environment Check')
                stage('pre-BDD Check') {
                    testStage.preBddCheck(deployContext)
                }
                milestone(label: 'Pre BDD Checks Complete')
                stage('upload Services') {
                    deployStage.uploadService(deployContext)
                }
                milestone(label: 'Uploaded Services')
                stage('deploy Services') {
                    deployStage.deployService(deployContext)
                }
                milestone(label: 'Deployed Services')
                stage('post-BDD Check') {
                    testStage.postBddCheck(deployContext)
                }
                milestone(label: 'Post BDD Checks Complete')
                stage('Test') {
                    testStage.deployTest(deployContext)
                }
                milestone(label: 'Test Services and Proxy')
                stage('Notify') {
                    notifyStage.deployNotify(deployContext)
                    notifyStage.splunkNotify('/apps/splunkreports/jenkinsstats/api')
                }
                break
            case 'bluemix':
                stage('deploy Services') {
                    deployStage.deployService(deployContext)
                }
                milestone(label: 'Deployed Services')
                stage('deploy Proxy') {
                    deployStage.deployProxy(deployContext)
                }
                milestone(label: 'Deployed Proxy')
                stage('Test') {
                    testStage.deployTest(deployContext)
                }
                milestone(label: 'Test Services and Proxy')
                stage('Notify') {
                    notifyStage.deployNotify(deployContext)
                }
                break
            default:
                def logger = new phoenixLogger()
                logger.printLog(" Error: No Deployment Type provided  ")
                throw new Exception("Error: No Deployment Type provided")
                break
        }
    } //End Lock
}

private def validate(deployContext) {
    isValid("journey", deployContext.journey)
    isValid("env", deployContext.env)
    isValid("proxy", deployContext.proxy)

    if (deployContext.deployment == null) {
        error "Invalid Configuration - deployment must be defined"

    }
    if (deployContext.services == null) {
        error "Invalid Configuration - services must be defined"
    }
}

private def isValid(name, value) {
    if (!value) {
        error "$name config must be defined"
    }
}

return this;
