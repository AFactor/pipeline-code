/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */
import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers
import com.lbg.workflow.sandbox.CWABuildHandlers

def call(BuildContext context, handlers, String targetBranch) {
	def unitTests = []
	def sanityTests = []
	def integrationTests = []
	def allTests = []

	def builder
	def appDeployer

	def targetEnv="integration"

	def epoch

	String integrationEnvironment = "${context.application}-${targetBranch}"


	node(){
		echo "TARGET_BRANCH: ${targetBranch}"
		epoch =	sh(returnStdout: true, script: 'date +%d%m%Y%H%M').trim()
		checkout scm
		
		echo "Loading all handlers"
		echo "Loading Builder: ${handlers.builder}"
		builder = load("${handlers.builder}")

		echo "Loading Deployer: ${handlers.deployer}"
		appDeployer = load(handlers.deployer)

		for (String test: handlers.getUnitTests()) {
			echo "Loading ${test}"
			unitTests.add( load("${test}"))
		}
		for (String test: handlers.getStaticAnalysis()) {
			echo "Loading ${test}"
			sanityTests.add( load("${test}"))
		}
		for (String test: handlers.getIntegrationTests()) {
			echo "Loading ${test}"
			integrationTests.add( load("${test}"))
		}


		allTests.addAll(unitTests)
		allTests.addAll(sanityTests)
		allTests.addAll(integrationTests)
	}
	milestone (label: 'Ready')
	// Try to test, and perform
	// post-build cleanup/publication regardless of failure
	try {
			// Basic Qualification -----------------------------------//
			stage("Unit Tests"){
				for (Object testClass: unitTests) {
					def currentTest = testClass
					currentTest.runTest(targetBranch, context)
				}
			}
			milestone (label: 'UnitTests')


			// Sonar/Checkstyle etal -----------------------------------//
			stage("Static Analysis"){
				def codeSanitySchedule = [:]
				for (Object testClass: sanityTests) {
					def currentTest = testClass
					codeSanitySchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
				}
				try{
					parallel codeSanitySchedule
				} catch(error) {
					//Make a decision
				} finally {
					//Make a decision
				}
			}
			milestone (label: 'StaticAnalysis')



			// Build--------------------------------------------------//
			stage("Build"){
				builder.pack(targetBranch, targetEnv, context)
			}
			milestone (label: 'Build')



			// Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
			lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment ) {
				// Integration Tests--------------------------------------//
				stage("Deploy"){
					appDeployer.deploy(targetBranch, context)  //Hardcoded to DEV as current practice
				}
				// Integration Tests--------------------------------------//
				stage("Integration Tests"){
					def integrationTestSchedule = [:]

					for (Object testClass: integrationTests) {
						def currentTest = testClass
						integrationTestSchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
					}
					try{
						parallel integrationTestSchedule
					} catch(error) {
						echo "Integration tests failed"
						throw error
					} finally {
						//Make a decision
					}
				}
				milestone (label: 'IntegrationTests')
			}

	} catch(error) { 
			echo "Some Mandatory Tests have failed. Aborting Build"
			throw error
	} finally {

			// Clean up environments/workspaces ----------------------//
			stage("Cleanup"){
				appDeployer.purge(targetBranch, context)
			}



			// Publish to 3rd Party Stacks----------------------------//
			stage("Publish"){
				try{
					builder.publishNexus(targetBranch, targetEnv, context)
				}catch(error){
					echo "Nexus publication did not complete normally. Continuing"
				} finally{

				}
				try {
					splunkPublisher{
						allTests = allTests
						epoch = epoch
						context = context
						targetBranch = targetBranch
					}
				} catch(error){
					echo "Splunk report publication did not complete normally. Continuing"
				} finally{

				}

			}
			stage("End"){ echo "Phew!. Finaly Finished" }
	}
}
return this;