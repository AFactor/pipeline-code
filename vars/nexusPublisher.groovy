/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def nexusURL = config.targetURL
	def artifact = config.tarball
	def credentialsID = 'nexus-uploader'

	withCredentials([
		usernameColonPassword(	credentialsId: credentialsID,
		variable: 'NEXUS_CREDS')
	]) { sh 	"""curl --insecure	-sS \
							-u $NEXUS_CREDS \
							--upload-file \
							${artifact} \
							${nexusURL}/${artifact} 
				"""  
	}
}

return this;
