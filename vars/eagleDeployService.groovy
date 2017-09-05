def call(service, deployContext) {
	node {
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

			// deploy
			if (deployContext.target == "apiconnect") {
				eagleDeployApiConnectService(service, deployContext)
			} else if (deployContext.target == "bluemix") {
				eagleDeployBluemixService(service, deployContext)
			}
		} catch(error){
			echo error.message
			throw error
		}finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

return this;
