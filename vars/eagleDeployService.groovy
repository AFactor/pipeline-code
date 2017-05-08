def call(service, deployContext) {
	echo "deploying service ${service.name}"
	node {
		checkout scm

		// fetch  artifact
		def artifact = service.runtime.binary.artifact
		def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
		echo "download artifact ${artifact}"
		sh """mkdir -p ${service.name} && \\
                  wget --quiet ${artifact} && \\
                  tar -xf ${artifactName} -C ${service.name}"""

		// deploy
		if (deployContext.target == "bluemix") {
			eagleDeployBluemixService(service, deployContext)
		}
	}
}

return this;