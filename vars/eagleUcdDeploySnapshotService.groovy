import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext) {
    echo "Deploying UCD Snapshot"

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def processMap = deployContext.platforms.ucd.process

    def entries = UDClient.mapAsList(processMap)
    for (def entry in entries) {
        def processValue = entry.get(1)

        def requestJson = createDeploySnapshotJson(deployContext, processValue)
        withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
            UtilsUCD utils = new UtilsUCD()
            def response = utils.deploy(ucdUrl, ucdToken, requestJson)
            echo("Deploy response: ${response}")

            def deploymentRequestId = UDClient.mapFromJson(response)['requestId']
            waitForDeploymentToFinish(ucdUrl, ucdToken, deploymentRequestId)
        }
    }
}


private createDeploySnapshotJson(deployContext, process) {

    def appName = deployContext.platforms.ucd.app_name
    def onlyChanged = deployContext.platforms.ucd.only_changed
    def environment = deployContext.release.environment
    def snapshotName = "${deployContext.releaseVersion()}.${env.BUILD_TIMESTAMP}"

    def jsonMap = [application          : appName,
                   applicationProcess   : process,
                   environment          : environment,
                   onlyChanged          : onlyChanged,
                   'post-deploy-message': '\${p:finalStatus}',
                   snapshot             : snapshotName]

    UDClient.jsonFromMap(jsonMap)
}

private waitForDeploymentToFinish(ucdUrl, ucdToken, requestId) {

    UtilsUCD utils = new UtilsUCD()

    def counter = 0
    def status = "NOTHING_YET"
    def result = "NOTHING_YET"

    while (status != "CLOSED") {
        def responseJson = utils.getDeploymentStatus(ucdUrl, ucdToken, requestId)
        def response = UDClient.mapFromJson(responseJson)
        status = response['status']
        result = response['result']

        echo("Status: ${status} :: Result: ${result}")
        if (["FAULTED", "CANCELED"].contains(result))
            break

        // 15 sec per run - 360 counts should be 1 hour 30 minutes
        // if it cannot deploy in 1 hour 30 minutes - there must be an issue
        if (counter > 360) {
            status == "TIMEOUT"
            break
        }
        counter++
        sleep(15)
    }

    echo("Result :: ${result}")
    echo("Status :: ${status}")
    utils.getRequestTrace(ucdUrl, ucdToken, requestId)

    if (status == "CLOSED" && result == "SUCCEEDED")
        return

    echo("UCD deployment failed, result: ${result}")
}

return this
