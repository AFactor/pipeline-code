/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

public interface BuildHandlers {

	def getBuilder()
	def getDeployer()
	def getUnitTests()
	def getStaticAnalysis()
	def getIntegrationTests()
}

