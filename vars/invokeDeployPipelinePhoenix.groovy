import com.lbg.workflow.sandbox.deploy.phoenix.DeployContext
import com.lbg.workflow.sandbox.deploy.phoenix.Service


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
                        phoenixDeployService(service, deployContext)
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
            switch(deployContext.deployment.type) {
                case 'ucd':
                    echo "Skipping Tests for Now"
                    break
                case 'bluemix':
                    try {
                        eagleDeployTester(deployContext)
                    } catch (error) {
                        echo "Test Stage Failure $error.message"
                        currentBuild.result = 'FAILURE'
                        notify(deployContext)
                        throw error
                    } finally {
                    }
                    break
                default:
                    println "************************************"
                    println " Error: No Deployment Type provided "
                    println "************************************"
                    currentBuild.result = 'FAILURE'
                    notify(deployContext)
                    throw new Exception("Error: No Deployment Type provided")
                    return null
            }
        }
        milestone(label: 'Test Services and Proxy')

    } //End Lock

    milestone(label: 'Notify')
    stage('Notify') {
        currentBuild.result = 'SUCCESS'
        notify(deployContext)
        echo "Finished"
    }

}

private def notify(deployContext) {
    if (currentBuild.result == 'SUCCESS' &&
            null != deployContext?.metadata?.confluence?.server &&
            null != deployContext?.metadata?.confluence?.page &&
            deployContext.metadata.confluence.server.trim() &&
            deployContext.metadata.confluence.page.trim()) {
        String credentials = deployContext.metadata.confluence.credentials
        echo "confluence notification"
        node {
            withCredentials([
                    usernameColonPassword(credentialsId: credentials,
                            variable: 'CONFLUENCE_CREDENTIALS')
            ]) {
                try {
                    for (Service service : deployContext.services) {
                        confluencePublisher(
                                deployContext.metadata.confluence.server,
                                deployContext.metadata.confluence.page,
                                "${env.CONFLUENCE_CREDENTIALS}",
                                buildConfluencePage(service, deployContext)
                        )
                    }
                } catch (error) {
                    echo "Confluence publisher failure $error.message"
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

private def buildConfluencePage(service, deployContext) {
    println "************************************"
    println " Running Confluence Page Generator "
    println "************************************"
    if (service.deploy) {
        switch (deployContext.metadata.confluence.type) {
            case 'cwa-ucd':
                def page = ucdPage(service, deployContext)
                echo "HTML Table: ${page}"
                return page
                break
            case 'bluemix':
                def page = bluemixPage(service, deployContext)
                echo "HTML Table: ${page}"
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

private def ucdPage(service, deployContext) {
    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def versionPath = "${version}-${revision}"
    def compNames = []
    artifactSet = new phoenixDeployService()
    artifactSet.createArtifactPath(service)
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def date = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))
    for (Object comp : service.components) {
        def components = comp
        compNames.add(components.name)
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

private def artifactTag(service) {
    switch(service.type){
        case 'cwa':
            def version = service.runtime.binary.version
            def revision = service.runtime.binary.revision
            def appName = "${version} - ${revision}"
            return appName
            break
        case 'bluemix':
            def artifact = service.runtime.binary.artifact
            def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
            return artifactName.split("artifact-")[1]
            break
        default: return null
    }
}

return this;
