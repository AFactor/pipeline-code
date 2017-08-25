import com.lbg.workflow.sandbox.deploy.phoenix.Service
import com.lbg.workflow.sandbox.JobStats

private def deployNotify(deployContext) {
    milestone(label: 'Notify')
    currentBuild.result = 'SUCCESS'
    notify(deployContext)
    phoenixLogger(3, "Finished :: SUCCESS", 'equal')
}

private def splunkNotify(String endPoint) {
    phoenixLogger(3, "Generating Splunk Stats", 'equal')
    def jobStats = new JobStats()
    jobStats.toSplunk(env.BUILD_TAG, env.BUILD_URL,
            'jenkins-read-all', currentBuild.result, endPoint)
}

private def buildParamsNotify(deployContext) {
    milestone(label: 'Notify')
    phoenixLogger(3, "Finished :: BUILT PARAMETERS :: SUCCESS", 'equal')
}

private def notify(deployContext) {
    phoenixLogger(3, "Running Notifications", 'equal')
    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.metadata?.confluence?.server &&
            null != deployContext?.metadata?.confluence?.page &&
            deployContext.metadata.confluence.server.trim() &&
            deployContext.metadata.confluence.page.trim()) {
        String credentials = deployContext.metadata.confluence.credentials
        phoenixLogger(4, "confluence notification", 'star')
        node() {
            withCredentials([
                    usernameColonPassword(credentialsId: credentials,
                            variable: 'CONFLUENCE_CREDENTIALS')
            ]) {
                try {
                    for (def service in deployContext.services) {
                        confluencePublisher(
                                deployContext.metadata.confluence.server,
                                deployContext.metadata.confluence.page,
                                "${env.CONFLUENCE_CREDENTIALS}",
                                buildConfluencePage(service, deployContext)
                        )
                    }
                } catch (error) {
                    phoenixLogger(1, "Confluence publisher failure $error.message", 'equal')
                    currentBuild.result = 'FAILURE'
                    throw error
                }
            }
        }
    }
    phoenixLogger(4, "Sending Emails", 'equal')
    if (null != deployContext.metadata.notifyList && deployContext.metadata.notifyList?.trim()) {
        phoenixLogger(5, "Email Notification", 'dash')
        emailNotify { to = deployContext.metadata.notifyList }
    }
}

private def buildConfluencePage(service, deployContext) {
    phoenixLogger(3, "Running Confluence Page Generator", 'equal')
    if (service.deploy) {
        switch (deployContext.metadata.confluence.type) {
            case 'ucd-cwa':
                def page = cwaPage(service, deployContext)
                phoenixLogger(5, "HTML Table: ${page}", "")
                return page
                break
            case 'ucd-api':
                def page = apiPage(service, deployContext)
                phoenixLogger(5, "HTML Table: ${page}", "")
                return page
                break
            case 'bluemix':
                def page = bluemixPage(service, deployContext)
                phoenixLogger(5, "HTML Table: ${page}", "")
                return page
                break
            default:
                return null

        }
    }
}

private def bluemixPage(service, deployContext) {
    String artifacts = ""
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    artifacts = artifacts + "<a href='$artifact'>$artifactName</a>" + "<br/>"
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

private def cwaPage(service, deployContext) {

    artifactSet = new phoenixDeployService()
    artifactSet.cwaArtifactPath(service)

    return UCDPage(service, deployContext)
}

private def apiPage(service, deployContext) {
    artifactSet = new phoenixDeployService()
    artifactSet.apiArtifactPath(service)

    return UCDPage(service, deployContext)
}

def UCDPage(service, deployContext){
    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def versionPath = "${version}-${revision}"
    def compNames = []
    artifactSet = new phoenixDeployService()
    artifactSet.apiArtifactPath(service)
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def date = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))
    for (def comp in service.components) {
        compNames.add(comp.name)
    }
    def page = """
    <table border="1">
    <tr>
       <td><strong>Date:</strong>${date}</td>
       <td><strong>Jenkins Build Number: </strong><a href="${env.BUILD_URL}">${env.BUILD_NUMBER}</a></td>
       <td><strong>Environment: </strong><br/>$deployContext.env</td>
       <td><strong>Component Version: </strong> ${version}</td>
       <td><strong>Component Revision: </strong> ${versionPath}</td>
       <td><strong>Components: </strong> ${compNames}</td>
       <td><strong>Artifact: </strong><a href="${artifact}">${artifactName}</a></td>
    </tr>
    </table> """
    return page
}
return this;
