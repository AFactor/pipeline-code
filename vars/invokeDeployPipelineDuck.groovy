import com.lbg.workflow.sandbox.deploy.duck.DatabaseDeployContext



def call(String application, handlers, configuration, String notifyList) {
    def unitTests = []
    def allTests = []

    def appDeployer
    String integrationEnvironment
    def success = false

    def targetEnv="integration"
    def targetBranch= "master"
    def deployContext
    def epoch



    stage("Initialize"){
        node('framework'){
            try {
                echo "TARGET_BRANCH: ${targetBranch}"
                epoch =	sh(returnStdout: true, script: 'date +%d%m%Y%H%M').trim()
                checkout scm
                echo "Loading all handlers"
                echo "Loading Deployer: ${handlers.deployer}"
                appDeployer = load(handlers.deployer)
                for (String test: handlers.getUnitTests()) {
                    echo "Loading ${test}"
                    unitTests.add( load("${test}"))
                }
                allTests.addAll(unitTests)
                try {
                    deployContext = new DatabaseDeployContext(readFile(configuration))
                   // validate(deployContext)
                    integrationEnvironment = "${deployContext.application}-${targetBranch}"

                } catch (error) {
                    echo "Invalid job configuration $error.message"
                    currentBuild.result = 'FAILURE'
                    notify(deployContext)
                    throw error
                }
                echo "Deploy Context " + deployContext.toString()
            }  catch(error) {
                echo error.message
                throw error
            } finally{
                step([$class: 'WsCleanup', notFailBuild: true])
            }
        }
        milestone (label: 'Initialized')
    }


    try {
        // Basic Qualification -----------------------------------//
        if(unitTests){
            stage("Unit Tests"){
                for (Object testClass: unitTests) {
                    def currentTest = testClass
                    currentTest.runTest(targetBranch, deployContext)
                }
            }
            milestone (label: 'UnitTests')
        }

        // Concurrency Controlled Deploy Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment ) {

                stage("Deploy"){
                    appDeployer.deploy(targetBranch, deployContext)
                }

            }

            success = true

    } catch(error) {
        echo "Some Mandatory Steps have failed. Aborting deployment"
        throw error
    } finally {

        // Clean up environments/workspaces ----------------------//
        stage("Cleanup"){
            try{
                appDeployer.purge(targetBranch, deployContext)
            }catch(error) {
                echo "Notice: Cleanup failed. Onwards!"
            } finally {}
        }
        stage("End"){ echo "Phew! Finally finished." }
    }

    if(notifyList?.trim()){
        emailNotify { to = notifyList }
    }
}



private def validate(deployContext) {
    isValid("journey", deployContext.journey)
    isValid("mavengoals", deployContext.mavengoals)
    isValid("nexus", deployContext.nexus)
    isValid("maven", deployContext.maven)
    isValid("metadata", deployContext.metadata)

}

private def isValid(name, value) {
    if (!value) {
        error "$name config must be defined"
    }
}

private def artifactTag(service) {
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    return artifactName.split("artifact-")[1]
}

return this;
