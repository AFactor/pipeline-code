import com.lbg.workflow.sandbox.BuildContext

def call(BuildContext context, handlers, String targetBranch) {
    def unitTests = []
    def sanityTests = []
    def integrationTests = []
    def allTests = []

    def builder
    def appDeployer

    def success = false

    def targetEnv = "integration"

    def epoch

    String integrationEnvironment = "${context.application}-${targetBranch}"

    stage("Initialize") {
        try {
            echo "TARGET_BRANCH: ${targetBranch}"
            epoch = sh(returnStdout: true, script: 'date +%d%m%Y%H%M').trim()

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
                echo "Loading ${test}"
                integrationTests.add(loadBuildHandler("${test}"))
            }

            allTests.addAll(unitTests)
            allTests.addAll(sanityTests)
            allTests.addAll(integrationTests)
        } catch (error) {
            echo error.message
            throw error
        } finally {
        }
        milestone(label: 'Ready')
    }
    try {
        // Basic Qualification -----------------------------------//
        if (unitTests) {
            stage("Unit Tests") {
                for (Object testClass : unitTests) {
                    def currentTest = testClass
                    currentTest.runTest(targetBranch, context)
                }
            }
            milestone(label: 'UnitTests')
        }

        // Sonar/Checkstyle et al -----------------------------------//
        if (sanityTests) {
            stage("Static Analysis") {
                def codeSanitySchedule = [:]
                for (Object testClass : sanityTests) {
                    def currentTest = testClass
                    codeSanitySchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
                }
                try {
                    parallel codeSanitySchedule
                    milestone(label: 'StaticAnalysis')
                } catch (error) {
                    echo "Static Analysis has failed."
                    throw error
                } finally {
                    //Make a decision
                }
            }
        }

        // Build--------------------------------------------------//
        stage("Package") {
            builder.pack(targetBranch, targetEnv, context)
        }
        milestone(label: 'Build')

        // Concurrency Controlled Deploy/IntegrationTest Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: integrationEnvironment) {
            // Integration Tests--------------------------------------//
            if (integrationTests) {
                stage("Deploy") {
                    appDeployer.deploy(targetBranch, context)  //Hardcoded to DEV as current practice
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
                        echo "Integration tests failed"
                        throw error
                    } finally {
                        //Make a decision
                    }
                }
            }

            success = true
        }
    } catch (error) {
        echo "Some Mandatory Steps have failed. Aborting Build"
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

        // Publish to 3rd Party Stacks----------------------------//
        stage("Publish") {
            if (success) {
                try {
                    builder.publishNexus(targetBranch, targetEnv, context)
                } catch (error) {
                    echo error.message
                    echo "Nexus publication did not complete normally. Continuing"
                    throw error
                } finally {

                }
            } else {
                echo "Build has failed. Not publishing to nexus"
            }
            //Publish what you can to splunk regardless of success.
            if (allTests) {
                try {
                    splunkPublisher {
                        tests = allTests
                        timestamp = epoch
                        buildContext = context
                        branchName = targetBranch
                    }
                } catch (error) {
                    echo error.message
                    echo "Splunk report publication did not complete normally. Continuing"
                } finally {
                }
            }

        }
        stage("End") { echo "Phew! Finally finished." }
    }
}

return this
