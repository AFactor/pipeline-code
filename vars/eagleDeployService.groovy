def call(service, deployContext) {
	echo "deploying service ${service.name}"
	node {
		try {
			checkout scm

			// fetch  artifact
			def artifact = service.runtime.binary.artifact
			def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
			echo "download artifact ${artifact}"
			sh """mkdir -p ${service.name} && \\
                  wget --quiet ${artifact} && \\
                  tar -xf ${artifactName} -C ${service.name}"""

			// deploy
			if (deployContext.target == "apiconnect") {
				eagleDeployApiConnectService(service, deployContext)
			} else if (deployContext.target == "bluemix") {
				eagleDeployBluemixService(service, deployContext)
			}
		} finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

return this;