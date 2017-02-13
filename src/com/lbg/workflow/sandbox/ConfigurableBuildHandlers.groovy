/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

class ConfigurableBuildHandlers implements Serializable,BuildHandlers{


	def builder
	def deployer 
	def unitTests = []
	def staticAnalysis = []
	def integrationTests = []

	public BuildHandlers(String builder, 
						String deployer, 
						String[] unitTests, 
						String[] staticAnalysis, 
						String[] integrationTests) { 

		this.builder = builder
		this.deployer = deployer
		this.unitTests = unitTests
		this.staticAnalysis = staticAnalysis
		this.integrationTests = integrationTests
	}

	public getBuilder() { return builder}
	public getDeployer() {return deployer}
	public getUnitTests() {return unitTests}
	public getStaticAnalysis() {return staticAnalysis}
	public getIntegrationTests() {return integrationTests}

}

