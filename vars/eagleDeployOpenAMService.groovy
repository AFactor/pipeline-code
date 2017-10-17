def call(service, deployContext) {
	if (service.buildpack == "Liberty") {
		echo "deploy openam service"
		def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
		dir(service.name) {
			def artifactName = sh(script: "ls *.zip| head -1", returnStdout: true).trim()
			def appHostName = sh(script: "hostname", returnStdout: true).trim()
			def appPort = service.deployment.openam['docker-port']
			def authenticationAppUrl = service.env['AUTHENTICATION_API_URL']
			def adpApiUrl = service.env['ADP_API_URL']
			def ardApiUrl = service.env['ARD_API_URL']
			def paymentServiceUrl = service.env['PAYMENT_SERVICE_API_URL']
			def outboundTlsTrustStore = service.env['OUTBOUND_TLS_TRUSTSTORE']
			def outboundTlsTrustStorePwd = service.env['OUTBOUND_TLS_TRUSTSTORE_PASSWORD']
			def outboundTlsKeyStore = service.env['OUTBOUND_TLS_KEYSTORE']
			def outboundTlsKeyStorePwd = service.env['OUTBOUND_TLS_KEYSTORE_PASSWORD']
			def amCryptoDescriptor = service.env['AM_CRYPTO_DESCRIPTOR']
			def amKeyDescriptor = service.env['AM_KEY_DESCRIPTOR']

			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
					"AUTHENTICATION_API_URL=${authenticationAppUrl}",
					"ADP_API_URL=${adpApiUrl}",
					"ARD_API_URL=${ardApiUrl}",
					"PAYMENT_SERVICE_API_URL=${paymentServiceUrl}",
					"OUTBOUND_TLS_TRUSTSTORE=${outboundTlsTrustStore}",
					"OUTBOUND_TLS_TRUSTSTORE_PASSWORD=${outboundTlsTrustStorePwd}",
					"OUTBOUND_TLS_KEYSTORE=${outboundTlsKeyStore}",
					"OUTBOUND_TLS_KEYSTORE_PASSWORD=${outboundTlsKeyStorePwd}",
					"AM_CRYPTO_DESCRIPTOR=${amCryptoDescriptor}",
					"AM_KEY_DESCRIPTOR=${amKeyDescriptor}",
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