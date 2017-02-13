/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

class CWABuildHandlers implements Serializable,BuildHandlers{


	def builder
	def deployer 
	def unitTests = []
	def staticAnalysis = []
	def integrationTests = []


	public CWABuildHandlers() {

		deployer = 'pipelines/deploy/application.groovy'
		builder = 'pipelines/build/package.groovy'

		unitTests.add( 'pipelines/tests/unit.groovy')

		staticAnalysis.add( 'pipelines/tests/sonar.groovy')
		staticAnalysis.add( 'pipelines/tests/checkstyle.groovy')

		integrationTests.add( 'pipelines/tests/performance.groovy')
		integrationTests.add( 'pipelines/tests/accessibility.groovy')
		integrationTests.add( 'pipelines/tests/bdd.groovy')
	}

	public getBuilder() { return builder}
	public getDeployer() {return deployer}
	public getUnitTests() {return unitTests}
	public getStaticAnalysis() {return staticAnalysis}
	public getIntegrationTests() {return integrationTests}

}

