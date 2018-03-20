/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

class CWABuildHandlers implements Serializable,BuildHandlers{
	String builder
	String deployer
	List<String> unitTests
	List<String> staticAnalysis
	List<String> integrationTests

	public CWABuildHandlers() {
		deployer = 'pipelines/deploy/application.groovy'
		builder = 'pipelines/build/package.groovy'

		unitTests = ['pipelines/tests/unit.groovy']

		staticAnalysis =  [	'pipelines/tests/sonar.groovy',
							'pipelines/tests/checkstyle.groovy']

		integrationTests = [ 	'pipelines/tests/performance.groovy',
								'pipelines/tests/accessibility.groovy',
								'pipelines/tests/bdd.groovy']
	}

/* Do not implement these methods. Accessors are xpected to be auto generated.
 * If we implement them, the build hangs

	public String getBuilder() { return builder}
	public String getDeployer() {return deployer}
	public List<String> getUnitTests() {return this.unitTests}
	public List<String> getStaticAnalysis() {return staticAnalysis}
	public List<String> getIntegrationTests() {return integrationTests}
*/
}
