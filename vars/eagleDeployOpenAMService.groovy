def call(service, deployContext) {
	if (service.type == "Liberty") {
		if (needsDeployment(service, deployContext)) {
			def artifact = service.runtime.binary.artifact
			echo "download artifact ${artifact}"
			sh """mkdir -p ${service.name} && \\
						cd ${service.name} && \\
                  		wget --quiet ${artifact}"""

			echo "deploy openam service"
			def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
			dir(service.name) {
				def artifactName = sh(script: "ls *.zip| head -1", returnStdout: true).trim()
				def appHostName = sh(script: "hostname", returnStdout: true).trim()
				def appPort = service.platforms.openam['docker-port']
				sh "unzip ${artifactName} wlp/usr/servers/* wlp/dev/tools/openam/*.sh "
				def tokens = buildTokens(service, deployContext)
				tokens['OPENAM_DOMAIN'] = "${appHostName}"
				tokens['OPENAM_SERVER_URL'] = "http://${appHostName}:${appPort}"
				tokens['OPENAM_LB_PRIMARY_URL'] = "http://${appHostName}:${appPort}/access-mgmt-service"
				replaceTokens('wlp/usr/servers', tokens)
				replaceTokens('wlp/dev/tools/openam', tokens)
				sh "zip ${artifactName}  wlp -r"
				withEnv([
						"APP=${appName}",
						"APP_HOSTNAME=${appHostName}",
						"APP_PORT=${appPort}",
						"WARFILE=${artifactName}",
						"ARTIFACT_VERSION=${getArtifactVersion(artifact)}",
						"OPENAM_DIRECTORY_PORT=${tokens['OPENAM_DIRECTORY_PORT']}",
						"OPENAM_DIRECTORY_ADMIN_PORT=${tokens['OPENAM_DIRECTORY_ADMIN_PORT']}",
						"OPENAM_DIRECTORY_JMX_PORT=${tokens['OPENAM_DIRECTORY_JMX_PORT']}"
				]) {
					try {
						def dockerFile = libraryResource 'com/lbg/workflow/sandbox/openam/Dockerfile'
						writeFile file: 'Dockerfile', text: dockerFile
						def deployScript = libraryResource 'com/lbg/workflow/sandbox/openam/deploy.sh'
						writeFile file: 'deploy.sh', text: deployScript
						sh "ls -lR"
						sh 'source ./deploy.sh; deployApp'
					} catch (error) {
						echo "Deployment failed"
						throw error
					} finally {
						step([$class: 'WsCleanup', notFailBuild: true])
					}
				}
			}
		}
	} else {
		error "Skipping service deployment, no implementation for type $service.type"
	}
}

def needsDeployment(service, deployContext) {
	def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def artifact = service.runtime.binary.artifact
	def artifactVersion = getArtifactVersion(artifact)
	withEnv([
			"APP=${appName}",
			"ARTIFACT_VERSION=${artifactVersion}"
	]) {
		try {
			String result = sh(script: "docker exec \$APP  sh -c 'printf \$ARTIFACT_VERSION'", returnStdout: true, returnStatus: false).trim()
			def resultVersion = (result ==~ /(?s)(.*)($artifactVersion)(.*)/)
			echo "version match: <$resultVersion>"
			return !(resultVersion)
		} catch (error) {
			echo "needs deployment failed $error.message"
			return true
		}
	}
}

def getArtifactVersion(artifact) {
	try {
		def arr = artifact.split("/")
		return arr[arr.length - 2]
	} catch (error) {
		echo error "$error"
		return ""
	}
}


private def buildTokens(service, deployContext) {
	def tokens = [:]
	tokens.putAll(deployContext?.platforms?.openam?.tokens ?: [:])
	tokens.putAll(service?.platforms?.openam?.tokens ?: [:])
	tokens.putAll(service?.tokens ?: [:])
	return tokens
}
