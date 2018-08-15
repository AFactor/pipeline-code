package com.lbg.workflow.sandbox

class JavaBuildHandlers implements Serializable,BuildHandlers {
	String builder
	String deployer
	List<String> unitTests
	List<String> staticAnalysis
	List<String> integrationTests

	public JavaBuildHandlers() {
		deployer  = 'pipelines/deploy/application.groovy'
		builder   = 'pipelines/build/package.groovy'
		unitTests = ['pipelines/tests/unit.groovy']
		staticAnalysis   = [ 'pipelines/tests/sonar.groovy']
		integrationTests = [ 'pipelines/tests/bdd.groovy', 'pipelines/tests/nexusIQ.groovy']
	}

/* Do not implement these methods. Accessors are expected to be auto generated.
 * If we implement them, the build hangs

	public String getBuilder() { return builder}
	public String getDeployer() {return deployer}
	public List<String> getUnitTests() {return this.unitTests}
	public List<String> getStaticAnalysis() {return staticAnalysis}
	public List<String> getIntegrationTests() {return integrationTests}
*/
}
