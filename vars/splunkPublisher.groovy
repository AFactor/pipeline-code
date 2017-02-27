/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
	
	def allTests = config.tests
	def epoch = config.timestamp
	def context = config.buildContext
	def targetBranch = config.branch

	node('framework'){

		for (Object testClass: allTests) {
			def currentTest = testClass
			echo "TRYING: Splunk publication for ${testClass.name()} "
			try{
				currentTest.publishSplunk(targetBranch, epoch, context, this)
				echo "SUCCESS: Splunk publication for ${testClass.name()} "
			} catch(error){
				echo error.message
				echo "FAILURE: Splunk publication for ${testClass.name()} "
			} finally {
			}
		}
	}
}

def SCP(String source, String destination) {
	echo "TRYING:  scp ${source}  splunk:${destination} "
	sshagent([this.credentialsID()]) { sh """scp 	-r \\
					-o StrictHostKeyChecking=no \\
					${source} \\
					splunk@10.113.140.187:${destination}"""  }
}

def RSYNC(String source, String destination) {
	echo "TRYING:  rsync ${source}  splunk:${destination} "
	sshagent([this.credentialsID()]) { sh """rsync -avz \\
					-e 'ssh -o StrictHostKeyChecking=no' \\
					${source} \\
					splunk@10.113.140.187:${destination}"""  }
}

def credentialsID() {
	return 'splunk-uploader'
}

return this;

