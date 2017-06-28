import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service

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
                notify(deployContext)
                throw error
            }
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
                currentBuild.result = 'FAILURE'
                notify(deployContext)
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
                    currentBuild.result = 'FAILURE'
                    notify(deployContext)
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
                currentBuild.result = 'FAILURE'
                notify(deployContext)
                throw error
            } finally {
            }
        }
        milestone(label: 'Test Services and Proxy')

    } //End Lock


    milestone(label: 'Tag')
    stage('Tag') {
        currentBuild.result = 'SUCCESS'
        tag(deployContext)
        echo "Finished"
    }


    milestone(label: 'Notify')
    stage('Notify') {
        currentBuild.result = 'SUCCESS'
        notify(deployContext)
        echo "Finished"
    }
}


private def tag(deployContext) {

    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.metadata?.tag?.enabled  &&
            deployContext.metadata.tag.enabled.toBoolean())
              {
        echo "git tag"
        node {
            // get the latest version (if any)
            sh 'git describe --tags $(git rev-list --tags --max-count=1) > version.tmp'
            def currentVersion = readFile 'version.tmp'
            def version =""

              echo 'found current tagged version ' + currentVersion
              try {

                      def microVersion = currentVersion.substring(currentVersion.lastIndexOf('.')+1) as int
                      version = currentVersion.substring(0, currentVersion.lastIndexOf('.')+1) + (microVersion+1)

                        echo "going from ${currentVersion} using new version " +  version
                        // push incremented tag
                        sh "git tag '${version}'"
                        sh 'git push --tags'

                        sh 'git describe --tags $(git rev-list --tags --max-count=1) > newversion.tmp'
                        def newVersion = readFile 'newversion.tmp'
                        echo 'tagged version ' + newVersion

                  } catch (err) {
                    echo "TAG: no existing tag found or tagging failed"

                  }

        }


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
    isValid("env", deployContext.env)
    isValid("target", deployContext.target)
    isValid("proxy", deployContext.proxy)
    if (deployContext.target == "bluemix") {
        isValid("bluemix", deployContext.bluemix)
    }
    if (deployContext.target == "apiconnect") {
        isValid("apiconnect", deployContext.apiconnect)
    }
    // TODO enforce stricter service validation?
    if (deployContext.services == null) {
        error "Invalid Configuration - services must be defined"
    }
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
