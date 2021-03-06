import com.lbg.workflow.sandbox.deploy.phoenix.DeployContext
import com.lbg.workflow.sandbox.deploy.phoenix.Service

private def uploadService(deployContext) {
    def uploads = [:]
    for (def service in deployContext.services) {
        if (service.upload) {
            echo "service $service.name"
            uploads["${service.name}: ${artifactTag(service)}"] = {
                phoenixDeployService(service, deployContext, "upload")
            }
        } else {
            echo "skipping service $service.name"
        }
    }
    try {
        echo "parallel deployments $uploads"
        parallel uploads
    } catch (error) {
        echo "Deploy Service Failure $error.message"
        currentBuild.result = 'FAILURE'
        phoenixNotifyStage().notify(deployContext)
        throw error
    } finally {
    }
}

private def deployService(deployContext) {
    def deployments = [:]
    for (Object serviceObject : deployContext.services) {
        Service service = serviceObject
        if (service.deploy) {
            echo "service $service.name"
            deployments["${service.name}: ${artifactTag(service)}"] = {
                phoenixDeployService(service, deployContext, "deploy")
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
        phoenixNotifyStage().notify(deployContext)
        throw error
    } finally {
    }
}

private def deployProxy(deployContext) {
    if (null != deployContext?.proxy?.deploy && deployContext.proxy.deploy == true) {
        try {
            eagleDeployProxy(deployContext)
        } catch (error) {
            echo "Deploy Proxy Failure  $error.message"
            currentBuild.result = 'FAILURE'
            phoenixNotifyStage().notify(deployContext)
            throw error
        } finally {
        }
    } else {
        echo "skipping proxy"
    }
}

private def artifactTag(service) {
    switch(service.type){
        case 'cwa':
            def appName = service.runtime.binary.artifactName
            return appName.split(/\./)[0]
            break
        case 'api':
            def appName = service.runtime.binary.artifactName
            return appName.split(/\./)[0]
            break
        case 'mca':
            def appName = service.runtime.binary.artifactName
            return appName.split(/\./)[0]
            break
        case 'salsa':
            def appName = service.runtime.binary.artifactName
            return appName.split(/\./)[0]
            break
        case 'ob-aisp':
            def appName = service.runtime.binary.artifactName
            return appName.split(/\./)[0]
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
