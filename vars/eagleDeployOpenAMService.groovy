def call(service, deployContext) {
	if (service.buildpack == "Liberty") {
		echo "deploy openam service"
		def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
		dir(service.name) {
			def artifactName = sh(script: "ls *.zip| head -1", returnStdout: true).trim()
			def appHostName = sh(script: "hostname", returnStdout: true).trim()
			def appPort = service.deployment.openam['docker-port']
			def authenticationAppUrl = service.env['authentication-url']
			def adpApiUrl = service.env['adp-url']
			def ardApiUrl = service.env['ard-url']
			def paymentServiceUrl = service.env['payment-service-url']
			withEnv([
					"APP=${appName}",
					"APP_HOSTNAME=${appHostName}",
					"APP_PORT=${appPort}",
					"WARFILE=${artifactName}",
					"AUTHENTICATION_API_URL=${authenticationAppUrl}",
					"ADP_API_URL=${adpApiUrl}",
					"ARD_API_URL=${ardApiUrl}",
					"PAYMENT_URL=${paymentServiceUrl}"
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