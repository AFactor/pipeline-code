/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */
import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers
import com.lbg.workflow.sandbox.Utils

def call(BuildContext context, handlers, String targetCommit) {
	def branchID = env.BRANCH_NAME.split('/')
	def revision = branchID[3]
	def changeID = branchID[2]

	// Target Branch Construction
	Utils  utils = new Utils()
	def gerritBranch = gerritHandler.findTargetBranch(targetCommit)

	def discriminator = ''
	if (gerritBranch.startsWith('sprint')){
		discriminator = 'ft-'
	}

	def friendlyGerritBranch = utils.friendlyName(gerritBranch)

	def targetBranch = "patchset-${discriminator}${friendlyGerritBranch}"
	// End Target Branch Construction

	def targetEnv="patchset"

	def unitTests = []
	def sanityTests = []
	def integrationTests = []

	def builder
	def appDeployer

	String integrationEnvironment = "${context.application}-${targetBranch}"


	node('framework'){
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
			def testClass = load("${test}")
			if (!test.toLowerCase().contains("fullbdd") && !testClass.name().toLowerCase().contains('fullbdd')) {
				echo "Loading ${test}"
				integrationTests.add(testClass)
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
				milestone (label: 'UnitTests')
			}catch(error){
				gerritHandler.failTests(changeID, revision)
				throw error
			}finally{
				//Nada
			}
		}



		// Sonar/Checkstyle etal -----------------------------------//
		stage("Static Analysis"){
			def codeSanitySchedule = [:]
			for (Object testClass: sanityTests) {
				def currentTest = testClass
				codeSanitySchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
			}
			try{
				parallel codeSanitySchedule
				gerritHandler.passCodeReview(changeID, revision)
				milestone (label: 'StaticAnalysis')
			} catch(error) {
				echo "Static Analysis has failed."
				gerritHandler.failCodeReview(changeID, revision)
				throw error
			} finally {
				//Make a decision
			}
		}




		// Build--------------------------------------------------//
		stage("Package"){
			try {
				builder.pack(targetBranch, targetEnv, context)
				milestone (label: 'Build')
			} catch(error){
				gerritHandler.failTests(changeID, revision)
				echo "BUilding Distribution failed"
				throw error
			} finally{
			}
		}




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
					milestone (label: 'IntegrationTests')
				} catch(error) {
					gerritHandler.failTests(changeID, revision)
					echo "Integration tests failed"
					throw error
				} finally {
					//Make a decision
				}
			}
		}

		gerritHandler.passTests(changeID, revision)

	} catch(error) {
		echo "Mandatory Steps have failed. Aborting"
		throw error
	} finally {
		// Clean up environments/workspaces ----------------------//
		stage("Cleanup"){
			try {
				appDeployer.purge(targetBranch, context)
			}catch(error) {
				echo "Notice: Cleanup failed. Onwards!"
			} finally{}
		}

		stage("End"){ echo "Phew!. Finaly Finished" }
	}

}
return this;