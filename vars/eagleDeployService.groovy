import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(service, deployContext) {
	def targetPlatform = targetPlatform(service)
	def nodeLabel = nodeLabel(service, deployContext, targetPlatform)
	node(nodeLabel) {
		try {
			checkout scm
			echo "target platform $targetPlatform "
			switch (targetPlatform) {
				case 'bluemix' : eagleDeployBluemixService(service, deployContext); break
				case 'apiconnect' : eagleDeployApiConnectService(service, deployContext); break
				case 'cmc' : eagleDeployCmcService(service, deployContext); break
				case 'openam' : eagleDeployOpenAMService(service, deployContext); break
			}
		} catch(error){
			echo error.message
			throw error
		}finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

def call(deployContext) {
	def nodeLabel = nodeLabel(deployContext)
	node(nodeLabel) {
		try {
			checkout scm
            new UtilsUCD().install_by_url(deployContext.platforms.ucd.ucd_url)

            // set git revision - ucd snapshot naming
			def targetCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
			env['GIT_COMMIT'] = targetCommit

			eagleUcdPropertyValidationService(deployContext)

			eagleUcdUploadArtifactsService(deployContext)

			eagleUcdUpdatePropertiesService(deployContext)

			def referenceSnapshot = eagleUcdCreateSnapshotService(deployContext)

			def environmentSnapshot = eagleUcdCreateEnvironmentSnapshotService(deployContext, referenceSnapshot)

			eagleUcdDeploySnapshotService(deployContext, environmentSnapshot)

		} catch (error) {
			echo(error.message)
			throw error
		} finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

def nodeLabel(service, deployContext, targetPlatform) {
	echo "nodeLabel service: $service, targetPlatform:$targetPlatform"
	if (null != service?.platforms?."$targetPlatform" && null != service?.platforms?."$targetPlatform"['node-label']) {
		return service.platforms."$targetPlatform"['node-label']
	}
	else if (null != deployContext?.platforms?."$targetPlatform" && null != deployContext?.platforms?."$targetPlatform"['node-label']) {
		return deployContext.platforms."$targetPlatform"['node-label']
	}
	else {
		return ""
	}
}

def nodeLabel(deployContext) {
	return null != deployContext?.platforms?.ucd ? deployContext.platforms.ucd['node-label'] : ""
}

def targetPlatform(service) {
	// service platforms have highest preference
	if (null != service?.platforms?.openam && null != service?.platforms?.openam) {
		return "openam"
	}
	else if (null != service?.platforms?.apiconnect && null != service?.platforms?.apiconnect) {
		return "apiconnect"
	}
	else if (null != service?.platforms?.cmc && null != service?.platforms?.cmc) {
		return "cmc"
	}
	return "bluemix"
}

return this
