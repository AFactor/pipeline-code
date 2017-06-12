
private def envCheck(deployContext) {
    return null
}

private def preBddCheck(deployContext) {
    return null
}
private def postBddCheck(deployContext) {
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
