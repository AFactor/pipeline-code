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
			replaceTokens('wlp/usr/servers', tokens)
			sh "zip ${artifactName}  wlp -r"

			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
					"OPENAM_DOMAIN=${service.tokens['OPENAM_DOMAIN']}",
					"OPENAM_PASSWORD=${service.tokens['OPENAM_PASSWORD']}",
					"OPENAM_COOKIE_DOMAIN=${service.tokens['OPENAM_COOKIE_DOMAIN']}",
					"OPENAM_SSL_OPTS=${service.tokens['OPENAM_SSL_OPTS']}",
					"OPENAM_LB_OPTS=${service.tokens['OPENAM_LB_OPTS']}",
					"OPENAM_LB_PRIMARY_URL=${service.tokens['OPENAM_LB_PRIMARY_URL']}",
					"OPENAM_LIBERTY_TRUSTSTORE=${service.tokens['OPENAM_LIBERTY_TRUSTSTORE']}",
					"OPENAM_LIBERTY_KEYSTORE=${service.tokens['OPENAM_LIBERTY_KEYSTORE']}",
					"OPENAM_LIBERTY_TRUSTSTORE_PASSWD=${service.tokens['OPENAM_LIBERTY_TRUSTSTORE_PASSWD']}",
					"OPENAM_LIBERTY_KEYSTORE_PASSWD=${service.tokens['OPENAM_LIBERTY_KEYSTORE_PASSWD']}",
					"OPENAM_DIRECTORY_PORT=${service.tokens['OPENAM_DIRECTORY_PORT']}",
					"OPENAM_DIRECTORY_ADMIN_PORT=${service.tokens['OPENAM_DIRECTORY_ADMIN_PORT']}",
					"OPENAM_DIRECTORY_PORT=${service.tokens['OPENAM_DIRECTORY_PORT']}",
					"OPENAM_DIRECTORY_ADMIN_PORT=${service.tokens['OPENAM_DIRECTORY_ADMIN_PORT']}",
					"OPENAM_DIRECTORY_JMX_PORT=${service.tokens['OPENAM_DIRECTORY_JMX_PORT']}",
					"OB_BRANDS=${service.tokens['OB_BRANDS']}",
					"OB_BRANDS_COOKIE_DOMAINS=${service.tokens['OB_BRANDS_COOKIE_DOMAINS']}"
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
	def tokens = service?.platforms?.openam?.tokens ?: [:]
	tokens.putAll(service?.tokens ?: [:])
	return tokens
}
