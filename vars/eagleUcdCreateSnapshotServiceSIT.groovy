import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient
import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.ServiceWrapper
import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service


def call(deployContext) {
    try {
        def timeoutInMinutes = getTimeout(deployContext)
        timeout(timeoutInMinutes){
            this.callHandler(deployContext)
            currentBuild.result = 'SUCCESS'
            milestone(label: 'Notify')
        }
    } catch(error) {
        currentBuild.result = 'FAILURE'
        milestone(label: 'Notify')
        throw error
    } finally {
            stage('Notify') {
            notify(deployContext)
            echo "Finished"
        }
    }
}

def callHandler(deployContext) {

    lock(inversePrecedence: true, quantity: 1, resource: "${deployContext.release.journey}-${deployContext.release.environment}-deploy") {

    def nodeLabel = nodeLabel(deployContext)
    node(nodeLabel) {
        try {
            checkout scm
            new UtilsUCD().install_by_url(deployContext.platforms.ucd.ucd_url)

            stage('Property Validate') {eagleUcdPropertyValidationService(deployContext)}

            stage('Artifact Upload') {eagleUcdUploadArtifactsService(deployContext)}

            stage('Property Update') {eagleUcdUpdatePropertiesService(deployContext)}

            stage('Create Snapshot') {
            def referenceSnapshot = eagleUcdCreateSnapshotService(deployContext)
	           environmentSnapshot = eagleUcdCreateEnvironmentSnapshotService(deployContext, referenceSnapshot)
				//eagleUcdDeploySnapshotService(deployContext, environmentSnapshot)
            }

        } catch (error) {
            echo(error.message)
            throw error
            } finally {
                step([$class: 'WsCleanup', notFailBuild: true])
        }
      }
    } //end lock
}  

def nodeLabel(deployContext) {
	return null != deployContext?.platforms?.ucd ? deployContext.platforms.ucd['node-label'] : ""
}

private def notify(deployContext) {
    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.release?.notifications?.confluence?.server &&
            null != deployContext?.release?.notifications?.confluence?.page &&
            deployContext.release.notifications.confluence.server.trim() &&
            deployContext.release.notifications.confluence.page.trim()) {
        echo "confluence notification"
        node {
            withCredentials([
                    usernameColonPassword(credentialsId: 'confluence-publisher', variable: 'CONFLUENCE_CREDENTIALS')
            ]) {
                try {
                    confluencePublisher(
                            deployContext.release.notifications.confluence.server,
                            deployContext.release.notifications.confluence.page,
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
            null != deployContext?.release?.notifications?.jira?.server &&
            deployContext.release.notifications.jira.server.trim())  {
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
                        jiraPublisher.addJiraComment(deployContext.release.notifications.jira.server,
                                jiraKey,
                                "${env.CONFLUENCE_CREDENTIALS}",
                                headline)
                        echo "SUCCESS: Jira Notification submitted "
                    }else
                    {
                        echo "FAILED: Jira, Could not find index key "
                    }

                } catch (error) {
                    echo "Jira publisher failure $error.message"
                    currentBuild.result = 'FAILURE'
                    throw error
                }
            }
        }
    }

    if (null != deployContext?.release?.notifications?.email && deployContext.release.notifications.email?.trim()) {
        echo "email notification"
        emailNotify { to = deployContext.release.notifications.email }
    }


}

private def buildConfluencePage(deployContext) {
    String artifacts = ""
    for (Service service : deployContext.services) {
        def artifact = service.runtime.binary.artifact
        def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
        artifacts = artifacts + "<a href='$artifact'>$artifactName</a>" + "<br/>"
    }
    def page = """
    <table border="1">
    <tr>
        <td><strong>Date</strong><br/>${new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))}</td>
        <td><strong>Job</strong><br/><a href="${env.BUILD_URL}">${env.JOB_BASE_NAME}</a></td>
        <td><strong>Environment</strong><br/>$deployContext.release.environment</td>
        <td><strong>Release Name</strong><br/>${deployContext?.release?.version}</td>
        <td><strong>Description</strong><br/>${deployContext?.release?.description}</td>
        <td><strong>Artifacts</strong><br/>$artifacts</td>
    </tr>
    </table>
    """
    return page
}

private def runParallel(deployContext) {
    return null == deployContext?.platforms?.ucd
}

private def getTimeout(deployContext) {
    deployContext?.platforms?.bluemix?.timeout ?: (deployContext?.platforms?.ucd?.timeout ?: 60)
}

private def artifactTag(service) {
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    if (service.type == "Node.js" && artifactName.contains("artifact-")) {
        return artifactName.split("artifact-")[1]
    }
    else if (service.type == "Liberty") {
        return artifactName.replaceAll(service.name + "-", "")
    }
    return artifactName
}

return this  	