/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def friendlyName(String branchName){
	return branchName.trim().split('/').last().trim().replaceAll("\\.","-")
}

// return name of assembly jar in maven projects using maven assembly plugin


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

def snapshotRelativeURL(String imagefile, String path){
	def code = libraryResource 'com/lbg/workflow/sandbox/js/snapshot.js'
	writeFile file: 'snapshot.js', text: code

	//This is from babel-polyfill@6.23.0
	def browsercode = libraryResource 'com/lbg/workflow/sandbox/js/browser.js'
	writeFile file: 'browser.js', text: browsercode

	def buildPath = relativeJobURL(path).replace(env.JENKINS_URL, '')
	withCredentials([
		usernamePassword(credentialsId: 'jenkins-read-all',
		passwordVariable: 'JENKINS_PASS',
		usernameVariable: 'JENKINS_USER')
	]) {
		withEnv([
			"JENKINS_URL=${env.JENKINS_URL}",
			"BUILD_PATH=${buildPath}",
			"IMAGEFILE=${imagefile}"
		]) { sh "which phantomjs >/dev/null || npm install phantomjs@2.1.7 ; phantomjs snapshot.js"  }
	}

}

def snapshotStatus(String imagefile){
  snapshotRelativeURL(imagefile, 'display/redirect')
}
