def call(service, deployContext) {
	def nodeLabel = getNodeLabel(service, deployContext)
	node(nodeLabel) {
		try {
			checkout scm
			echo "deploying service ${service.name}"
			// fetch  artifact
			def artifact = service.runtime.binary.artifact
			def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
			echo "download artifact ${artifact}"

			if (service.buildpack == "Node.js") {
				sh """mkdir -p ${service.name} && \\
                  wget --quiet ${artifact} && \\
                  tar -xf ${artifactName} -C ${service.name}"""
			} else if (service.buildpack == "Liberty") {
				sh """mkdir -p ${service.name} && \\
                  cd ${service.name} && \\
                  wget --quiet ${artifact}"""
			} else if (service.buildpack == "Staticfile") {
				sh """mkdir -p ${service.name} && \\
					wget --quiet ${artifact} &&	 \\
					tar -xf ${artifactName} -C ${service.name}"""

			} else {
				error "Skipping service deployment, no implementation for buildpack ${service.buildpack}"
			}

			// deploy - service platform has highest order
			if (null != service.deployment && service.deployment.openam) {
				eagleDeployOpenAMService(service, deployContext)
			} else {
				if (deployContext.target == "apiconnect") {
					eagleDeployApiConnectService(service, deployContext)
				} else if (deployContext.target == "cmc") {
					eagleDeployCmcService(service, deployContext)
				} else if (deployContext.target == "bluemix") {
					eagleDeployBluemixService(service, deployContext)
				}
			}
		} catch(error){
			echo error.message
			throw error
		}finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

def getNodeLabel(service, deployContext) {
	if (null != service.deployment) {
		if (service.deployment.openam) {
			return service.deployment.openam['node-label']
		}
		return ""
	} else {
		return ""
	}
}

return this;
