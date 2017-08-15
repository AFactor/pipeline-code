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

	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.bluemix, deployContext.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.journey}-${service.name}-${deployContext.env}"
	bluemixEnvs["ZIPFILE"] = sh(script: "cd  ${env.WORKSPACE}/${service.name} && ls *.zip| head -1", returnStdout: true).trim()
	withCredentials([
		usernamePassword(credentialsId: deployContext.bluemix.credentials,
		passwordVariable: 'BM_PASS',
		usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
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
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs())
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
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs())
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

private def buildAnalyticsEnvs() {
	def envs = [:]
	def analytics = [
		"ANALYTICS_HOST_NAME",
		"ANALYTICS_ACCOUNT_NAME",
		"ANALYTICS_ACCESS_KEY",
		"ANALYTICS_APP_NAME"
	]
	for (String credentialsId : analytics) {
		def credentialsValue = getCredentials(credentialsId)
		if (credentialsValue) {
			envs[credentialsId] = credentialsValue
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
