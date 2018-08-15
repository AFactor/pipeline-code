import com.lbg.workflow.sandbox.*

def call(BuildContext context, handlers, String targetCommit) {
	def branchID = env.BRANCH_NAME.split('/')
	def revision = branchID[3]
	def changeID = branchID[2]

	// Target Branch Construction
	Utils utils = new Utils()
	def gerritBranch = gerritHandler.findTargetBranch(targetCommit)

	def discriminator = ''
	if (gerritBranch.startsWith('sprint')){
		discriminator = 'ft-'
	}

	def friendlyGerritBranch = utils.friendlyName(gerritBranch)
	def targetBranch = "patchset-${discriminator}${friendlyGerritBranch}"
	// End Target Branch Construction

	def builder          = handlers.builder
	def appDeployer      = handlers.appDeployer
	def unitTests        = handlers.unitTests
	def sanityTests      = handlers.sanityTests
	def integrationTests = handlers.integrationTests

	def targetEnv="patchset"
	String integrationEnvironment = "${context.application}-${targetBranch}"
	print "PatchsetWorkFlow for ${targetCommit}, TARGET_BRANCH: ${targetBranch}..."

	try {
		// Basic Qualification -----------------------------------//
		if (unitTests){
			stage("Unit Tests"){
				try {
					for (Object testClass: unitTests){
						def currentTest = testClass
						currentTest.runTest(targetBranch, context)
					}
					milestone (label: 'UnitTests')
				} catch(error){
					echo "failed unit tests."
					echo error.message
					gerritHandler.failTests(changeID, revision)
					throw error
				}
			}
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
					gerritHandler.passCodeReview(changeID, revision)
					milestone (label: 'StaticAnalysis')
				} catch(error) {
					echo "Static Analysis has failed."
					gerritHandler.failCodeReview(changeID, revision)
					throw error
				}
			}
		}

		// Build------only if deployment needed---------------------------//
		if (integrationTests){
			stage("Package"){
				try {
					builder.pack(targetBranch, targetEnv, context)
					milestone (label: 'Build')
				} catch(error){
					gerritHandler.failTests(changeID, revision)
					echo "BUilding Distribution failed"
					throw error
				}
			}
		}

		// Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
		lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment){
			// Integration Tests--------------------------------------//
			if(integrationTests){
				stage("Deploy"){
					try {
						appDeployer.deploy(targetBranch, context)
					} catch(error){
						gerritHandler.failTests(changeID, revision)
						echo "Deployment failed"
						throw error
					}
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
					} catch(error){
						gerritHandler.failTests(changeID, revision)
						echo "Integration tests failed"
						throw error
					}
				}
			}
		}
		gerritHandler.passTests(changeID, revision)
	} catch(error) {
		echo "Aborting build - some mandatory steps have failed:"
		echo error.message
		throw error
	} finally {
		// Clean up environments/workspaces ----------------------//
		stage("Cleanup"){
			try {
				appDeployer.purge(targetBranch, context)
			} catch(error){
				echo "Notice: Cleanup failed. Onwards!"
			}
		}
		stage("End"){ echo "Phew! Finally finished." }
	}
}

return this;
