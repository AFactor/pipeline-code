/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

public interface BuildHandlers {
	String getBuilder()
	String getDeployer()
	List<String> getUnitTests()
	List<String> getStaticAnalysis()
	List<String> getIntegrationTests()
}
