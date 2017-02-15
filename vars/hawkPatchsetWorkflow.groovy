/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */
import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers

def call(BuildContext context, handlers, String targetCommit) {
	def branchID = env.BRANCH_NAME.split('/')
	def revision = branchID[3]
	def changeID = branchID[2]

	def gerritBranch = gerritHandler.findTargetBranch(targetCommit)
	def targetBranch = "patchset-${gerritBranch}"
	def targetEnv="patchset"

	def unitTests = []
	def sanityTests = []
	def integrationTests = []

	def builder
	def appDeployer

	String integrationEnvironment = "${context.application}-${targetBranch}"


	node(){
		echo "TARGET_BRANCH: ${targetBranch}"

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
			if (!test.contains("bdd")) {
				echo "Loading ${test}"
				integrationTests.add( load("${test}"))
			}
		}

		gerritHandler.buildStarted(changeID,revision)
	}
	milestone (label: 'Ready')
	try{
		// Basic Qualification -----------------------------------//
		stage("Unit Tests"){
			try {
				for (Object testClass: unitTests) {
					def currentTest = testClass
					currentTest.runTest(targetBranch, context)
				}
			}catch(error){
				gerritHandler.failTests(changeID, revision)
				throw error
			}finally{
				//Nada
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
				gerritHandler.failCodeReview(changeID, revision)
			} finally {
				//Make a decision
			}
		}
		milestone (label: 'StaticAnalysis')



		// Build--------------------------------------------------//
		stage("Build"){
			try {
				builder.pack(targetBranch, targetEnv, context)
			} catch(error){
				gerritHandler.failTests(changeID, revision)
				echo "BUilding Distribution failed"
				throw error
			} finally{
			}
		}
		milestone (label: 'Build')



		// Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
		lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment ) {
			// Integration Tests--------------------------------------//
			stage("Deploy"){
				try{
					appDeployer.deploy(targetBranch, context)  //Hardcoded to DEV as current practice
				} catch(error) {
					gerritHandler.failTests(changeID, revision)
					echo "Deployment failed"
					throw error
				} finally{
				}
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
					gerritHandler.failTests(changeID, revision)
					echo "Integration tests failed"
					throw error
				} finally {
					//Make a decision
				}
			}
			milestone (label: 'IntegrationTests')

		}


	} catch(error) {
		echo "Mandatory Tests have failed. Aborting"
		throw error
	} finally {
		// Clean up environments/workspaces ----------------------//
		stage("Cleanup"){
			gerritHandler.passTests(changeID, revision)
			appDeployer.purge(targetBranch, context)
		}

		stage("End"){ echo "Phew!. Finaly Finished" }
	}

}
return this;