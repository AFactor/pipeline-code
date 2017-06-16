import com.lbg.workflow.sandbox.deploy.phoenix.Service

private def envCheck(deployContext) {
    return null
}

private def bddTests(deployContext, testFiles, milestone) {
    for (Object serviceObject : deployContext.services) {
        Service service = serviceObject
        if (service.deploy) {
            switch(service.type) {
                case 'api':
                    apiBddCheck(deployContext, service, testFiles, milestone)
                    break
                case 'salsa':
                    phoenixLogger(3, "Currently No Tests Defined For: ${milestone} :: Skipping", 'star')
                    //salsaBddCheck(deployContext, service, testFiles, milestone)
                    break
                case 'cwa':
                    phoenixLogger(3, "Currently No Tests Defined For: ${milestone} :: Skipping", 'star')
                    break
                default:
                    phoenixLogger(1, " Error: No Service Type provided  ", 'star')
                    throw new Exception("Error: No Service Type provided")
                    break
            }
        } else {
            phoenixLogger(3, "Deployment Is Disabled Skipping ${milestone}", 'star')
        }
    }

}

private def salsaBddCheck(deployContext, service, testFiles) {
    return null
}

private def apiBddCheck(deployContext, service, testFiles, milestone) {
    def tests = []
    def testSchedule = [:]
    for (String test: testFiles) {
        echo "Loading ${test}"
        tests.add( load("${test}"))
    }

    for (Object testClass: tests) {
        def currentTest = testClass
        version = service.runtime.binary.version
        testSchedule[currentTest.name()] = { currentTest.runTest(version, deployContext) }
    }
    try{
        parallel testSchedule
        milestone (label: milestone)
    } catch (error) {
        phoenixLogger(1, "api BDD Test Stage Failure $error.message", 'star')
        currentBuild.result = 'FAILURE'
        notify(deployContext)
        throw error
    } finally {
    }
    return null
}

private def deployTest(deployContext) {
    switch(deployContext.deployment.type) {
        case 'ucd':
            echo "Skipping Tests for Now"
            break
        case 'bluemix':
            try {
                eagleDeployTester(deployContext)
            } catch (error) {
                phoenixLogger(1, "Test Stage Failure $error.message", 'star')
                currentBuild.result = 'FAILURE'
                notify(deployContext)
                throw error
            } finally {
            }
            break
        default:
            phoenixLogger(1, "No Deployment Type provided", 'star')
            currentBuild.result = 'FAILURE'
            notify(deployContext)
            throw new Exception("Error: No Deployment Type provided")
            break
    }
}

return this;
