import com.lbg.workflow.sandbox.deploy.ManifestBuilder
import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(service, deployContext) {
	if (needsDeployment(service, deployContext)) {
		echo "deploying service ${service.name}"
		def artifact = service.runtime.binary.artifact
		def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
		switch (service.type) {
			case "Node.js":
				echo "download artifact ${artifact}"
				sh """mkdir -p ${service.name} && \\
                  		wget --quiet ${artifact} && \\
                  		tar -xf ${artifactName} -C ${service.name}""";
				echo "deploy node service ${service.name}"
				nodeBuildPack(service, deployContext); break

			case "Liberty":
				echo "download artifact ${artifact}"
				sh """mkdir -p ${service.name} && \\
						cd ${service.name} && \\
                  		wget --quiet ${artifact}""";
				echo "deploy liberty service ${service.name}"
				libertyBuildPack(service, deployContext); break

			case "Java":
				echo "download artifact ${artifact}"
				sh """mkdir -p ${service.name} && \\
						cd ${service.name} && \\
                  		wget --quiet ${artifact}""";
				echo "deploy java service ${service.name}"
				javaBuildPack(service, deployContext); break

			case "Staticfile":
				echo "download artifact ${artifact}"
				sh """mkdir -p ${service.name} && \\
						wget --quiet ${artifact} &&	 \\
						tar -xf ${artifactName} -C ${service.name}""";
				echo "deploy service ${service.name}"
				staticfileBuildPack(service, deployContext); break

			default:
				error "Service type ${service.type} not supported"
		}
	} else {
		echo "service ${deployContext.release.journey}-${service.name}-${deployContext.release.environment} already exists"
	}
}

private void libertyBuildPack(service, deployContext) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))
	manifest = manifestBuilder.buildEnvs(manifest, ["ARTIFACT_VERSION": getArtifactVersion(service.runtime.binary.artifact)])
	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.platforms.bluemix, deployContext.platforms.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def artifactName = sh(script: "cd  ${env.WORKSPACE}/${service.name} && ls *.zip| head -1", returnStdout: true).trim()
	bluemixEnvs["ZIPFILE"] = artifactName
	withCredentials([
			usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
					passwordVariable: 'BM_PASS',
					usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				def tokens = buildTokens(service, deployContext)
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

private void javaBuildPack(service, deployContext) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, ["ARTIFACT_VERSION": getArtifactVersion(service.runtime.binary.artifact)])
	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.platforms.bluemix, deployContext.platforms.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def artifactName = sh(script: "cd  ${env.WORKSPACE}/${service.name} && ls *.*ar| head -1", returnStdout: true).trim()
	bluemixEnvs["ARCHIVE"] = artifactName
	withCredentials([
			usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
					passwordVariable: 'BM_PASS',
					usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				def tokens = buildTokens(service, deployContext)
				sh "mkdir -p pipelines/scripts/"
				writeFile file: "pipelines/scripts/deploy.sh", text: deployJavaAppScript()
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

private void nodeBuildPack(service, deployContext) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))
	manifest = manifestBuilder.buildEnvs(manifest, ["ARTIFACT_VERSION": getArtifactVersion(service.runtime.binary.artifact)])
	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.platforms.bluemix, deployContext.platforms.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	withCredentials([
			usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
					passwordVariable: 'BM_PASS',
					usernameVariable: 'BM_USER')
	]) {
		withEnv(utils.toWithEnv(bluemixEnvs)) {
			try {
				def tokens = buildTokens(service, deployContext)
				if (tokens.size() > 0) {
					if (fileExists("${env.WORKSPACE}/${service.name}/urbanCode")) {
						replaceTokens("${env.WORKSPACE}/${service.name}/urbanCode", tokens)
						sh("cp -rf ${env.WORKSPACE}/${service.name}/urbanCode/* ${env.WORKSPACE}/${service.name}/  2>/dev/null || : && cp -rf ${env.WORKSPACE}/${service.name}/urbanCode/.* ${env.WORKSPACE}/${service.name}/ 2>/dev/null || :")
					} else {
						replaceTokens("${env.WORKSPACE}/${service.name}", tokens)
					}
				}
                if (null != deployContext?.platforms?.bluemix?.types?."$service.type"?.prune) {
                    sh "rm -f ${env.WORKSPACE}/${service.name}/${deployContext.platforms.bluemix.types."$service.type".prune}"
                }
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

private void staticfileBuildPack(service, deployContext) {
	// build manifest
	echo "build service manifest"
	def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	def manifestBuilder = new ManifestBuilder()
	def manifest = manifestBuilder.build(appName, service, deployContext)
	manifest = manifestBuilder.buildEnvs(manifest, buildAnalyticsEnvs(deployContext))
	manifest = manifestBuilder.buildEnvs(manifest, ["ARTIFACT_VERSION": getArtifactVersion(service.runtime.binary.artifact)])

	sh "mkdir -p ${service.name}/pipelines/conf"
	writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

	// deploy service
	echo "deploy service"
	def utils = new UtilsBluemix()
	def bluemixEnvs = utils.buildServiceBluemixEnv(service.platforms.bluemix, deployContext.platforms.bluemix)
	bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
	bluemixEnvs["APP"] = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
	withCredentials([
			usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
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
	if (null != deployContext.platforms.bluemix.analytics && deployContext.platforms.bluemix.analytics == true) {
		def analytics = [
				"ANALYTICS_HOST_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_HOST_NAME",
				"ANALYTICS_ACCOUNT_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_ACCOUNT_NAME",
				"ANALYTICS_ACCESS_KEY" : "CI/users/OB/APPDYNAMICS/ANALYTICS_ACCESS_KEY",
				"ANALYTICS_APP_NAME" : "CI/users/OB/APPDYNAMICS/ANALYTICS_APP_NAME",
				"ENABLE_DEBUG_ANALYTICS" : "CI/users/OB/APPDYNAMICS/ENABLE_DEBUG_ANALYTICS",
				"APPDYNAMICS_PORT" : "CI/users/OB/APPDYNAMICS/APPDYNAMICS_PORT"
		]
		try {
			withCredentials([string(credentialsId: deployContext.platforms.bluemix.analytics_credentials, variable: 'VAULT_TOKEN')]) {
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


private def buildTokens(service, deployContext) {
	def tokens = deployContext?.platforms?.bluemix?.types?."$service.type"?.tokens ?: [:]
	tokens.putAll(service?.platforms?.bluemix?.tokens ?: [:])
	tokens.putAll(service?.tokens ?: [:])
	return tokens
}

private String deployLibertyAppScript() {
	libraryResource "com/lbg/workflow/sandbox/bluemix/deploy-liberty-app.sh"
}

private String deployJavaAppScript() {
	libraryResource "com/lbg/workflow/sandbox/bluemix/deploy-java-app.sh"
}

private def deployNodeAppScript() {
	libraryResource "com/lbg/workflow/sandbox/bluemix/deploy-nodejs-app.sh"
}

private def deployStaticfileAppScript() {
	libraryResource "com/lbg/workflow/sandbox/bluemix/deploy-staticfile-app.sh"
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

def needsDeployment(service, deployContext) {
	try {
		def utils = new UtilsBluemix()
		def bluemixEnvs = utils.buildServiceBluemixEnv(service.platforms.bluemix, deployContext.platforms.bluemix)
		def appName = "${deployContext.release.journey}-${service.name}-${deployContext.release.environment}"
		String artifactVersion = "ARTIFACT_VERSION: ${getArtifactVersion(service.runtime.binary.artifact)}"
		withCredentials([
				usernamePassword(credentialsId: deployContext.platforms.bluemix.credentials,
						passwordVariable: 'BM_PASS',
						usernameVariable: 'BM_USER')
		]) {
			withEnv(utils.toWithEnv(bluemixEnvs)) {
				String result = sh(script: "cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV 1>/dev/null && cf app ${appName} && cf env ${appName}", returnStdout: true, returnStatus: false).trim()
				def resultStatus = (result ==~ /(?s)(.*)(requested state:(\s+)started)(.*)/)
				def resultVersion = (result ==~ /(?s)(.*)($artifactVersion)(.*)/)
				echo "app started: <$resultStatus>,  version match: <$resultVersion>"
				return !(resultStatus && resultVersion)

			}
		}
	} catch (error) {
		echo "needsDeployment failed, error $error.message"
		return true
	}
}
