def call(service, deployContext) {
	if (service.buildpack == "Liberty") {
		echo "deploy openam service"
		def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
		dir(service.name) {
			def artifactName = sh(script: "ls *.zip| head -1", returnStdout: true).trim()
			def appHostName = sh(script: "hostname", returnStdout: true).trim()
			def appPort = service.deployment.openam['docker-port']
			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
					"AUTHENTICATION_API_URL=${service.env['AUTHENTICATION_API_URL']}",
					"ADP_API_URL=${service.env['ADP_API_URL']}",
					"ARD_API_URL=${service.env['ARD_API_URL']}",
					"PAYMENT_SERVICE_API_URL=${service.env['PAYMENT_SERVICE_API_URL']}",
					"OUTBOUND_TLS_TRUSTSTORE=${service.env['OUTBOUND_TLS_TRUSTSTORE']}",
					"OUTBOUND_TLS_TRUSTSTORE_PASSWORD=${service.env['OUTBOUND_TLS_TRUSTSTORE_PASSWORD']}",
					"OUTBOUND_TLS_KEYSTORE=${service.env['OUTBOUND_TLS_KEYSTORE']}",
					"OUTBOUND_TLS_KEYSTORE_PASSWORD=${service.env['OUTBOUND_TLS_KEYSTORE_PASSWORD']}",
					"AM_CRYPTO_DESCRIPTOR=${service.env['AM_CRYPTO_DESCRIPTOR']}",
					"AM_KEY_DESCRIPTOR=${service.env['AM_KEY_DESCRIPTOR']}",
					"ERROR_THRESHOLD_PERCENTAGE=${service.env['ERROR_THRESHOLD_PERCENTAGE']}",
					"MINIMUM_REQUEST_FOR_HEATH_CHECK=${service.env['MINIMUM_REQUEST_FOR_HEATH_CHECK']}",
					"REQUEST_TIMEOUT_IN_MILLISECONDS=${service.env['REQUEST_TIMEOUT_IN_MILLISECONDS']}",
					"OPEN_CIRCUIT_TIMEOUT_IN_MILLISECONDS=${service.env['OPEN_CIRCUIT_TIMEOUT_IN_MILLISECONDS']}",
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
		error "Skipping service deployment, no implementation for buildpack $service.buildpack"
	}
}