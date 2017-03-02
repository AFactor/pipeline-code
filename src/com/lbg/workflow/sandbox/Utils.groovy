/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

@NonCPS
def friendlyName(String branchName){
	return branchName.trim().split('/').last().trim()
}

@NonCPS
def friendlyName(String branchName, int maxsize){
	//Wonky implementation due to
	//org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException:
	//Scripts not permitted to use staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods

	def buffer = friendlyName(branchName)
	int size = buffer.size()
	if (size > maxsize) {
		return buffer[(0-maxsize)..-1]
	} else {
		return buffer
	}
}

def snapshotStatus(String imagefile){

	def code = libraryResource 'com/lbg/workflow/sandbox/js/snapshot.js'
	writeFile file: 'snapshot.js', text: code
	sh "cat snapshot.js"
	def buildPath = env.BUILD_URL.replace(env.JENKINS_URL, '')
	echo buildPath
	echo env.BUILD_URL
	echo env.JENKINS_URL
	echo "gooooooooooooooooooooooooooooooooooooooooooooo"
	withCredentials([
		usernamePassword(credentialsId: 'bluemix-global-deployer',
		passwordVariable: 'JENKINS_PASS',
		usernameVariable: 'JENKINS_USER')
	]) {
		withEnv([
			"BUILD_PATH=${buildPath}",
			"IMAGEFILE=${imagefile}"
		]) { sh """npm install phantomjs@2.1.7  babel-polyfill@6.23.0 && \
                   node_modules/.bin/phantomjs snapshot.js
                """  }
	}

}