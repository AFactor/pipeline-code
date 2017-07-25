import com.lbg.workflow.sandbox.deploy.duck.DatabaseDeployContext


def call(String application, handlers, String configuration){
    this.call(application, handlers, configuration
}

def call(String application, handlers, configuration) {
    def unitTests = []
    def allTests = []

    def appDeployer
    String integrationEnvironment
    def success = false

    def targetEnv="integration"
    def targetBranch= "master"

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
                for (String test: handlers.getIntegrationTests()) {
                    echo "Loading ${test}"
                    integrationTests.add( load("${test}"))
                }
                allTests.addAll(unitTests)
                allTests.addAll(integrationTests)
                try {
                    deployContext = new DatabaseDeployContext(readFile(configuration))
                    validate(deployContext)
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
        if(!unitTests.empty){
            stage("Unit Tests"){
                for (Object testClass: unitTests) {
                    def currentTest = testClass
                    currentTest.runTest(targetBranch, context)
                }
            }
            milestone (label: 'UnitTests')
        }

        // Concurrency Controlled Deploy Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment ) {

                stage("Deploy"){
                    appDeployer.deploy(targetBranch, context)  //Hardcoded to DEV as current practice
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
                appDeployer.purge(targetBranch, context)
            }catch(error) {
                echo "Notice: Cleanup failed. Onwards!"
            } finally {}
        }
        stage("End"){ echo "Phew!. Finally Finished" }
    }
}



private def notify(deployContext) {

    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.metadata?.confluence?.server &&
            null != deployContext?.metadata?.confluence?.page &&
            deployContext.metadata.confluence.server.trim() &&
            deployContext.metadata.confluence.page.trim()) {
        echo "confluence notification"
        node {
            withCredentials([
                    usernameColonPassword(credentialsId: 'confluence-publisher', variable: 'CONFLUENCE_CREDENTIALS')
            ]) {
                try {
                    confluencePublisher(
                            deployContext.metadata.confluence.server,
                            deployContext.metadata.confluence.page,
                            "${env.CONFLUENCE_CREDENTIALS}",
                            buildConfluencePage(deployContext)
                    )
                } catch (error) {
                    echo "Confluence publisher failure $error.message"
                    currentBuild.result = 'FAILURE'
                    throw error
                }
            }
        }
    }

    // jira notifier
    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.metadata?.jira?.server &&
            deployContext.metadata.jira.server.trim())  {
        echo "JIRA notification"
        node {
            withCredentials([
                    usernameColonPassword(credentialsId: 'confluence-publisher', variable: 'CONFLUENCE_CREDENTIALS')
                    //shared jira/confluence credentials
            ]) {
                try {

                  def headline = globalUtils.urlDecode(
                      "J2:${env.JOB_NAME}:${env.BUILD_NUMBER}-> ${currentBuild.result}")
                      fullBranch= ${env.BRANCH_NAME}
                      int index = fullBranch.lastIndexOf("/");
                      String issueKey = fullBranch.substring(index + 1);

                      if(issueKey != null && !issueKey.isEmpty()) {
                        jiraPublisher.addJiraComment(deployContext.metadata.jira.server,
                          jiraKey,
                          "${env.CONFLUENCE_CREDENTIALS}",
                          headline)
                        echo "SUCCESS: Jira Notification submitted "
                      }else
                      {
                        echo "FAILED: Jira, Couldn't find index key "
                      }

                } catch (error) {
                    echo "Jira publisher failure $error.message"
                    currentBuild.result = 'FAILURE'
                    throw error
                }
            }
        }
    }


    if (null != deployContext.metadata.notifyList && deployContext.metadata.notifyList?.trim()) {
        echo "email notification"
        emailNotify { to = deployContext.metadata.notifyList }
    }


}

private def buildConfluencePage(deployContext) {
    String artifacts = ""
    for (Service service : deployContext.services) {
        if (service.deploy) {
            def artifact = service.runtime.binary.artifact
            def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
            artifacts = artifacts + "<a href='$artifact'>$artifactName</a>" + "<br/>"
        }
    }
    def page = """
    <table border="1">
    <tr>
        <td><strong>Date</strong><br/>${new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))}</td>
        <td><strong>Job</strong><br/><a href="${env.BUILD_URL}">${env.JOB_BASE_NAME}</a></td>
        <td><strong>Environment</strong><br/>$deployContext.env</td>
        <td><strong>Target</strong><br/>$deployContext.target</td>
        <td><strong>Release Name</strong><br/>${deployContext?.metadata?.name}</td>
        <td><strong>Description</strong><br/>${deployContext?.metadata?.description}</td>
        <td><strong>Artifacts</strong><br/>$artifacts</td>
    </tr>
    </table>
    """
    return page
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
