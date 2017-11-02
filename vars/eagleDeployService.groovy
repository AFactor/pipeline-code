import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(service, deployContext) {
	def targetPlatform = targetPlatform(service)
	def nodeLabel = nodeLabel(service, deployContext, targetPlatform)
	node(nodeLabel) {
		try {
			checkout scm
			echo "deploying service ${service.name}"
			// fetch  artifact
			def artifact = service.runtime.binary.artifact
			def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
			echo "download artifact ${artifact}"

			switch (service.type) {
				case "Node.js":
					sh """mkdir -p ${service.name} && \\
                  		wget --quiet ${artifact} && \\
                  		tar -xf ${artifactName} -C ${service.name}"""; break

				case "Liberty":
					sh """mkdir -p ${service.name} && \\
						cd ${service.name} && \\
                  		wget --quiet ${artifact}"""; break

				case "Staticfile":
					sh """mkdir -p ${service.name} && \\
						wget --quiet ${artifact} &&	 \\
						tar -xf ${artifactName} -C ${service.name}"""; break

				default: error "Service type ${service.type} not supported"
			}

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

			if (deployContext.platforms.ucd.dry_run) {
				eagleUcdDryRunService(deployContext)
				return
			}

            // set build timestamp  - needed ucd snapshot naming
			env['BUILD_TIMESTAMP'] = new Date().format("yyyyMMdd-HH:mm:ss", TimeZone.getTimeZone('UTC'))

			eagleUcdUploadArtifactsService(deployContext)

			eagleUcdUpdatePropertiesService(deployContext)

			eagleUcdCreateSnapshotService(deployContext)

			eagleUcdDeploySnapshotService(deployContext)

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
		return deployContext."$targetPlatform"['node-label']
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
