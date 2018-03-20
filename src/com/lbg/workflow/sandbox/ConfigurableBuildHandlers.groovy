/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

class ConfigurableBuildHandlers implements Serializable,BuildHandlers {
	String builder
	String deployer
	List<String> unitTests
	List<String> staticAnalysis
	List<String> integrationTests

	public ConfigurableBuildHandlers(String builder,
						String deployer,
						List<String> unitTests,
						List<String> staticAnalysis,
						List<String> integrationTests) {

		this.builder = builder
		this.deployer = deployer
		this.unitTests = unitTests
		this.staticAnalysis = staticAnalysis
		this.integrationTests = integrationTests
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
