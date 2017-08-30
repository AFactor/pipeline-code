/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def nexus = config.nexusAPI ?: 'https://nexus.sandbox.extranet.group/nexus/service/local/artifact/maven/content'
	def file = config.artifactPath ?: 'missing artifact'
	def userpass = config.credentialsId?: 'nexus-uploader'
	def group = config.groupId ?: 'no-group-id'
	def artifact = config.artifactId ?: 'no-artifact-id'
	def semver = config.version ?: '0.0.1'
	def pack	= config.packaging ?: 'zip'
	def repo	= config.repository ?: 'releases'


	withCredentials([
		usernameColonPassword(	credentialsId: userpass,
		variable: 'NEXUS_CREDS')
	]) {
		sh """curl 	-sS -u $NEXUS_CREDS \\
						-F r=${repo}  \\
						-F p=${pack}   \\
						-F g=${group}   \\
						-F a=${artifact} \\
						-F v=${semver} \\
						-F file=@${file} ${nexus}
				"""
	}
}

return this;
