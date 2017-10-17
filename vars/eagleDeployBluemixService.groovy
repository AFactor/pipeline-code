import com.lbg.workflow.sandbox.deploy.ManifestBuilder
import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(service, deployContext) {
	if (service.buildpack == "Node.js") {
		nodeBuildPack(deployContext, service)
	} else if (service.buildpack == "Liberty") {
		libertyBuildPack(deployContext, service)
	} else if (service.buildpack == "Staticfile") {
		staticfileBuildPack(deployContext, service)
	} else {
		error "Skipping service deployment, no implementation for buildpack $service.buildpack"
	}
}

private void libertyBuildPack(deployContext, service) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))

	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.bluemix, deployContext.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.journey}-${service.name}-${deployContext.env}"
	def artifactName = sh(script: "cd  ${env.WORKSPACE}/${service.name} && ls *.zip| head -1", returnStdout: true).trim()
	bluemixEnvs["ZIPFILE"] = artifactName
	withCredentials([
		usernamePassword(credentialsId: deployContext.bluemix.credentials,
		passwordVariable: 'BM_PASS',
		usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				def tokens = service.tokens?: [:]
				if (tokens.size() > 0) {
					dir("${env.WORKSPACE}/${service.name}") {
						sh "unzip ${artifactName} wlp/usr/servers/* "
						replaceTokens('wlp/usr/servers', tokens)
						sh "zip ${artifactName}  wlp -r"
					}
				}
				sh "mkdir -p pipelines/scripts/"
				writeFile file: "pipelines/scripts/deploy.sh", text: deployLibertyAppScript()
				sh 'source pipelines/scripts/deploy.sh; deployApp'
			} catch (error) {
				echo "Deployment failed"
				throw error
			} finally {
				step([$class: 'WsCleanup', notFailBuild: true])
			}
		}
	}
}

private void nodeBuildPack(deployContext, service) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))
	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.bluemix, deployContext.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.journey}-${service.name}-${deployContext.env}"
	withCredentials([
		usernamePassword(credentialsId: deployContext.bluemix.credentials,
		passwordVariable: 'BM_PASS',
		usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				sh "mkdir -p pipelines/scripts/"
				writeFile file: "pipelines/scripts/deploy.sh", text: deployNodeAppScript()
				sh 'source pipelines/scripts/deploy.sh; deployApp'
			} catch (error) {
				echo "Deployment failed"
				throw error
			} finally {
				step([$class: 'WsCleanup', notFailBuild: true])
			}
		}
	}
}

private void staticfileBuildPack(deployContext, service) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))
	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.bluemix, deployContext.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.journey}-${service.name}-${deployContext.env}"
	withCredentials([
		usernamePassword(credentialsId: deployContext.bluemix.credentials,
		passwordVariable: 'BM_PASS',
		usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				def tokens= service.tokens?: [:]
				replaceTokens('.', tokens)
				sh "mkdir -p pipelines/scripts/"
				writeFile file: "pipelines/scripts/deploy.sh", text: deployStaticfileAppScript()
				sh 'source pipelines/scripts/deploy.sh; deployApp'
			} catch (error) {
				echo "Deployment failed"
				throw error
			} finally {
				step([$class: 'WsCleanup', notFailBuild: true])
			}
		}
	}
}

private def buildAnalyticsEnvs(deployContext) {
	def envs = [:]
	if (null != deployContext.bluemix.analytics && deployContext.bluemix.analytics == true) {
		def analytics = [
				"ANALYTICS_HOST_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_HOST_NAME",
				"ANALYTICS_ACCOUNT_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_ACCOUNT_NAME",
				"ANALYTICS_ACCESS_KEY" : "CI/users/OB/APPDYNAMICS/ANALYTICS_ACCESS_KEY",
				"ANALYTICS_APP_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_APP_NAME",
				"ENABLE_DEBUG_ANALYTICS" : "CI/users/OB/APPDYNAMICS/ENABLE_DEBUG_ANALYTICS",
				"APPDYNAMICS_PORT" : "CI/users/OB/APPDYNAMICS/APPDYNAMICS_PORT"
		]
		try {
			withCredentials([string(credentialsId: deployContext.bluemix.analytics_credentials, variable: 'VAULT_TOKEN')]) {
				withGenericVaultSecrets(analytics) {
					envs["ANALYTICS_HOST_NAME"] = env.ANALYTICS_HOST_NAME
					envs["ANALYTICS_ACCOUNT_NAME"] = env.ANALYTICS_ACCOUNT_NAME
					envs["ANALYTICS_ACCESS_KEY"] = env.ANALYTICS_ACCESS_KEY
					envs["ANALYTICS_APP_NAME"] = env.ANALYTICS_APP_NAME
					envs["ENABLE_DEBUG_ANALYTICS"] = env.ENABLE_DEBUG_ANALYTICS
					envs["APPDYNAMICS_PORT"] = env.APPDYNAMICS_PORT
				}
			}
		} catch (error) {
			echo "Failed to fetch vault keys : ${error.message}"
		}
	}
	return envs
}

private String deployLibertyAppScript() {
	return """
        #!/bin/bash
        set -ex

        function deployApp() {
            echo 'deploying liberty app'
            cd \$deployable
            cat pipelines/conf/manifest.yml
            cf logout
            cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV
            cf delete \${APP} -f -r || echo "Failed to delete application."
            cf push \${APP} -f pipelines/conf/manifest.yml -p \${ZIPFILE} -t 180
            sleep 60
        }
    """
}


private String deployNodeAppScript() {
	return """
        #!/bin/bash
        set -ex

        function deployApp() {
            cd \$deployable
            cat pipelines/conf/manifest.yml
            cf logout
            cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV
            cf delete \${APP} -f -r || echo "Failed to delete application."
            cf push -f pipelines/conf/manifest.yml
        }
    """
}

private String deployStaticfileAppScript() {
	return """
        #!/bin/bash
        set -ex

        function deployApp() {
            cd \$deployable
            cat pipelines/conf/manifest.yml
            cf logout
            cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV
            cf delete \${APP} -f -r || echo "Failed to delete application."
            cf push -f pipelines/conf/manifest.yml
        }
    """
}

private def getCredentials(id) {
	def pwd
	try {
		withCredentials([
			usernamePassword(credentialsId: id,
			passwordVariable: 'credentialsPwd',
			usernameVariable: id)
		]) { pwd =  env.credentialsPwd }
	} catch (error) {
		echo "Failed to locate credentials with id : $id"
	}
	return pwd
}
