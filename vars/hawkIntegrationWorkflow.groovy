import com.lbg.workflow.sandbox.*

def call(BuildContext context, handlers, String targetBranch) {
	def builder          = handlers.builder
	def appDeployer      = handlers.appDeployer
	def unitTests        = handlers.unitTests
	def sanityTests      = handlers.sanityTests
	def integrationTests = handlers.integrationTests
	def allTests = unitTests + sanityTests + integrationTests
	def success = false

	def targetEnv="integration"
	String integrationEnvironment = "${context.application}-${targetBranch}"
	print "IntegrationWorkFlow for ${targetBranch}..."

	// Try to test, and perform
	// post-build cleanup/publication regardless of failure
	try {
		// Basic Qualification -----------------------------------//
		if(unitTests){
			stage("Unit Tests"){
				for (Object testClass: unitTests) {
					def currentTest = testClass
					currentTest.runTest(targetBranch, context)
				}
			}
			milestone (label: 'UnitTests')
		}

		// Sonar/Checkstyle etal -----------------------------------//
		if (sanityTests){
			stage("Static Analysis"){
				def codeSanitySchedule = [:]
				for (Object testClass: sanityTests){
					def currentTest = testClass
					codeSanitySchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
				}
				try {
					parallel codeSanitySchedule
					milestone (label: 'StaticAnalysis')
				} catch(error){
					echo "Static Analysis has failed."
					throw error
				}
			}
		}

		// Build--------------------------------------------------//
		stage("Package"){
			builder.pack(targetBranch, targetEnv, context)
		}
		milestone (label: 'Build')

		// Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
		lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment){
			// Integration Tests--------------------------------------//
			if (integrationTests){
				stage("Deploy"){
					appDeployer.deploy(targetBranch, context)
				}
				// Integration Tests--------------------------------------//
				stage("Integration Tests"){
					def integrationTestSchedule = [:]

					for (Object testClass: integrationTests){
						def currentTest = testClass
						integrationTestSchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
					}
					try {
						parallel integrationTestSchedule
						milestone (label: 'IntegrationTests')
					} catch(error) {
						echo "Integration tests failed"
						throw error
					}
				}
			}
			success = true
		}
	} catch(error) {
		echo "Aborting build - some mandatory steps have failed:"
		echo error.message
		throw error
	} finally {

		// Clean up environments/workspaces ----------------------//
		stage("Cleanup"){
			try{
				appDeployer.purge(targetBranch, context)
			} catch(error){
				echo "Notice: Cleanup failed. Onwards!"
			}
		}

		// Publish to 3rd Party Stacks----------------------------//
		stage("Publish"){
			if (success){
				try {
					builder.publishNexus(targetBranch, targetEnv, context)
				} catch(error){
					echo "Nexus publication failed:"
					echo error.message
					throw error
				}
			} else {
				echo "Build has failed. Not publishing to nexus."
			}

			//Publish what you can to splunk regardless of success.
			if (allTests){
				try {
					splunkPublisher {
						tests = allTests
						buildContext = context
						branchName = targetBranch
					}
				} catch(error){
					echo "Splunk report publication did not complete normally:"
					echo error.message
				}
			}
		}
		stage("End"){ echo "Phew! Finally finished." }
	}
}

return this;
