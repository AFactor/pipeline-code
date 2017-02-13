/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

@NonCPS
def friendlyName(String branchName){
	return branchName.trim().split('/').last()
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
