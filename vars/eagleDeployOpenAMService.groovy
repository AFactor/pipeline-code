def call(service, deployContext) {
	if (service.type == "Liberty") {
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


			echo "service - platforms ${service.platforms}"
			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
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
	} else {
		error "Skipping service deployment, no implementation for type $service.type"
	}
}

private def buildTokens(service, deployContext) {
	def tokens = deployContext?.platforms?.openam?.tokens ?: [:]
	tokens.putAll(service?.platforms?.openam?.tokens ?: [:])
	tokens.putAll(service?.tokens ?: [:])
	return tokens
}
