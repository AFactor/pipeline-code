/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def nexusURL = config.nexusURL
	def artifact = config.artifact
	def credentialsID = config.credentials ?: 'nexus-uploader'

	withCredentials([
		usernameColonPassword(	credentialsId: credentialsID,
		variable: 'NEXUS_CREDS')
	]) { sh 	"""curl 	-sS \
							-u $NEXUS_CREDS \
							--upload-file \
							${artifact} \
							${nexusURL}/${artifact} 
				"""  
	}
}

return this;
