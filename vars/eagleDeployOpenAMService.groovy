def call(service, deployContext) {
	if (service.type == "Liberty") {
		echo "deploy openam service"
		def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
		dir(service.name) {
			def artifactName = sh(script: "ls *.zip| head -1", returnStdout: true).trim()
			def appHostName = sh(script: "hostname", returnStdout: true).trim()
			def appPort = service.deployment.openam['docker-port']
			sh "unzip ${artifactName} wlp/usr/servers/* wlp/dev/tools/openam/*.sh "
			replaceTokens('wlp/usr/servers', service.env)
			sh "zip ${artifactName}  wlp -r"

			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
					"OPENAM_DOMAIN=${service.env['OPENAM_DOMAIN']}",
					"OPENAM_PASSWORD=${service.env['OPENAM_PASSWORD']}",
					"OPENAM_COOKIE_DOMAIN=${service.env['OPENAM_COOKIE_DOMAIN']}",
					"OPENAM_SSL_OPTS=${service.env['OPENAM_SSL_OPTS']}",
					"OPENAM_LB_OPTS=${service.env['OPENAM_LB_OPTS']}",
					"OPENAM_LB_PRIMARY_URL=${service.env['OPENAM_LB_PRIMARY_URL']}",
					"OPENAM_LIBERTY_TRUSTSTORE=${service.env['OPENAM_LIBERTY_TRUSTSTORE']}",
					"OPENAM_LIBERTY_KEYSTORE=${service.env['OPENAM_LIBERTY_KEYSTORE']}",
					"OPENAM_LIBERTY_TRUSTSTORE_PASSWD=${service.env['OPENAM_LIBERTY_TRUSTSTORE_PASSWD']}",
					"OPENAM_LIBERTY_KEYSTORE_PASSWD=${service.env['OPENAM_LIBERTY_KEYSTORE_PASSWD']}",
					"OPENAM_DIRECTORY_PORT=${service.env['OPENAM_DIRECTORY_PORT']}",
					"OPENAM_DIRECTORY_ADMIN_PORT=${service.env['OPENAM_DIRECTORY_ADMIN_PORT']}",
					"OPENAM_DIRECTORY_PORT=${service.env['OPENAM_DIRECTORY_PORT']}",
					"OPENAM_DIRECTORY_ADMIN_PORT=${service.env['OPENAM_DIRECTORY_ADMIN_PORT']}",
					"OPENAM_DIRECTORY_JMX_PORT=${service.env['OPENAM_DIRECTORY_JMX_PORT']}"
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