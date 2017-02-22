/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def setStatus (String changeID, String revision, String message, String codereview, String verified) {

	node(){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--code-review=${codereview}  \\
				--label verified=${verified}
				"""

		sshagent(['gerrit-updater']) { sh command }
	}
}

def setCodeReview (String changeID, String revision, String message, String codereview) {

	node(){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--code-review=${codereview} 
				"""

		sshagent(['gerrit-updater']) { sh command }
	}
}
def setVerified (String changeID, String revision, String message, String verified) {

	node(){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--label verified=${verified} 
				"""

		sshagent(['gerrit-updater']) { sh command }
	}
}

def buildStarted(String changeID, String revision){
	setStatus (changeID, revision, 'Build Started', '0', '0')
}

def failCodeReview (String changeID, String revision ) {
	setCodeReview (changeID, revision, "Code Quality Failed", '-2')
}
def failTests (String changeID, String revision) {
	setVerified (changeID, revision, "Tests Failed", '-1')
}
def passCodeReview (String changeID, String revision ) {
	setCodeReview (changeID, revision, "Code Quality Passed", '+1')
}
def passTests (String changeID, String revision) {
	setVerified (changeID, revision, "Tests Passed", '+1')
}



String findTargetBranch(String targetCommit) {


	def targetBranch = ''
	def command = """
		ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
			gerrit query --format=text \\
				${targetCommit} \\
		| awk '/^([ ]+branch:)(.*)/{print \$2}'
       """
	node(){
		sshagent(['gerrit-updater']) {
			targetBranch = sh(returnStdout: true, script: command).trim()
		}
	}

	switch (targetBranch) {
		case "master": return "master" ; break
		case "release-prod": return "release-prod" ; break
		case "hotfixes": return "hotfixes" ; break
		default: return "ignore" ; break
	}
}

return this;