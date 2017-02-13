/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
	
	def allTests = config.allTests
	def epoch = config.epoch
	def context = config.context
	def targetBranch = targetBranch

	node(){

		for (Object testClass: allTests) {
			def currentTest = testClass
			try{
				currentTest.publishSplunk(targetBranch, epoch, context, this)
			} catch(error){
			} finally {
			}
		}
	}
}

def SCP(String source, String destination) {
	sshagent([this.credentialsID()]) { sh """scp 	-r \\
					-o StrictHostKeyChecking=no \\
					${source} \\
					splunk@10.113.140.187:${destination}"""  }
}

def RSYNC(String source, String destination) {
	sshagent([this.credentialsID()]) { sh """rsync -avz \\
					-e 'ssh -o StrictHostKeyChecking=no' \\
					${source} \\
					splunk@10.113.140.187:${destination}"""  }
}

def credentialsID() {
	return 'splunk-uploader'
}

return this;

