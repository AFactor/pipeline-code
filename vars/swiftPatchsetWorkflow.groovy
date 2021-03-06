import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.Utils

def call(BuildContext context, handlers, String targetCommit) {
    def branchID = env.BRANCH_NAME.split('/')
    def revision = branchID[3]
    def changeID = branchID[2]

    // Target Branch Construction
    Utils utils = new Utils()
    def gerritBranch = gerritHandler.findTargetBranch(targetCommit)

    def discriminator = ''
    if (gerritBranch.startsWith('sprint')) {
        discriminator = 'ft-'
    }

    def friendlyGerritBranch = utils.friendlyName(gerritBranch)

    def targetBranch = "patchset-${discriminator}${friendlyGerritBranch}"
    // End Target Branch Construction

    def targetEnv = "patchset"

    def unitTests = []
    def sanityTests = []
    def integrationTests = []

    def builder
    def appDeployer

    String integrationEnvironment = "${context.application}-${targetBranch}"

    stage("Initialize") {
        try {
            echo "TARGET_BRANCH: ${targetBranch}"

            echo "Loading all handlers"
            echo "Loading Builder: ${handlers.builder}"
            builder = loadBuildHandler("${handlers.builder}")

            echo "Loading Deployer: ${handlers.deployer}"
            appDeployer = loadBuildHandler(handlers.deployer)

            for (String test : handlers.getUnitTests()) {
                echo "Loading ${test}"
                unitTests.add(loadBuildHandler("${test}"))
            }
            for (String test : handlers.getStaticAnalysis()) {
                echo "Loading ${test}"
                sanityTests.add(loadBuildHandler("${test}"))
            }
            for (String test : handlers.getIntegrationTests()) {
                def testClass = loadBuildHandler("${test}")
                if (!test.toLowerCase().contains("fullbdd") && !testClass.name().toLowerCase().contains('fullbdd')) {
                    echo "Loading ${test}"
                    integrationTests.add(testClass)
                }
            }
        } catch (error) {
            echo error.message
            throw error
        } finally {
        }
        gerritHandler.buildStarted(changeID, revision)
        milestone(label: 'Ready')
    }
    try {
        // Basic Qualification -----------------------------------//
        if (unitTests) {
            stage("Unit Tests") {
                try {
                    for (Object testClass : unitTests) {
                        def currentTest = testClass
                        currentTest.runTest(targetBranch, context)
                    }
                    milestone(label: 'UnitTests')
                } catch (error) {
                    gerritHandler.failTests(changeID, revision)
                    throw error
                } finally {
                }
            }
        }

        // Sonar/Checkstyle etal -----------------------------------//
        if (sanityTests) {
            stage("Static Analysis") {
                def codeSanitySchedule = [:]
                for (Object testClass : sanityTests) {
                    def currentTest = testClass
                    codeSanitySchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
                }
                try {
                    parallel codeSanitySchedule
                    gerritHandler.passCodeReview(changeID, revision)
                    milestone(label: 'StaticAnalysis')
                } catch (error) {
                    echo "Static Analysis has failed."
                    gerritHandler.failCodeReview(changeID, revision)
                    throw error
                } finally {
                }
            }
        }

        // Build------only if deployment needed---------------------------//
        if (integrationTests) {
            stage("Package") {
                try {
                    builder.pack(targetBranch, targetEnv, context)
                    milestone(label: 'Build')
                } catch (error) {
                    gerritHandler.failTests(changeID, revision)
                    echo "BUilding Distribution failed"
                    throw error
                } finally {
                }
            }
        }

        // Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment) {
            // Integration Tests--------------------------------------//
            if (integrationTests) {
                stage("Deploy") {
                    try {
                        appDeployer.deploy(targetBranch, context)  //Hardcoded to DEV as current practice
                    } catch (error) {
                        gerritHandler.failTests(changeID, revision)
                        echo "Deployment failed"
                        throw error
                    } finally {
                    }
                }
                // Integration Tests--------------------------------------//
                stage("Integration Tests") {
                    def integrationTestSchedule = [:]

                    for (Object testClass : integrationTests) {
                        def currentTest = testClass
                        integrationTestSchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
                    }
                    try {
                        parallel integrationTestSchedule
                        milestone(label: 'IntegrationTests')
                    } catch (error) {
                        gerritHandler.failTests(changeID, revision)
                        echo "Integration tests failed"
                        throw error
                    } finally {
                    }
                }
            }
        }
        gerritHandler.passTests(changeID, revision)

    } catch (error) {
        echo "Mandatory Steps have failed. Aborting"
        throw error
    } finally {
        // Clean up environments/workspaces ----------------------//
        stage("Cleanup") {
            try {
                appDeployer.purge(targetBranch, context)
            } catch (error) {
                echo "Notice: Cleanup failed. Onwards!"
            } finally {
            }
        }

        stage("End") { echo "Phew! Finally finished." }
    }

}

return this
