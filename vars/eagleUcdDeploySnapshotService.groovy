import com.lbg.workflow.global.GlobalUtils
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.UDClient

def call(deployContext, snapshotName) {
    deploySnapshot(deployContext, snapshotName)
}

private deploySnapshot(deployContext, snapshotName) {
    echo "Deploying UCD Snapshot ${snapshotName}"

    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials
    def processMap = deployContext.platforms.ucd.process
    def environment = deployContext.release.environment
    UtilsUCD utils = new UtilsUCD()

    def deployStreams = [failFast: false]
    def entries = GlobalUtils.mapAsList(processMap)
    for (def entry in entries) {
        String processKey = entry.get(0)
        String processValue = entry.get(1)

        deployStreams[processKey] = {
            def requestJson = createDeploySnapshotJson(deployContext, processValue, snapshotName)
            echo("Starting ${processValue}, deploy snapshot json: ${requestJson}")

            withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->

                def response
                lock(inversePrecedence: true, quantity: 1, resource: "${deployContext.release.journey}-${environment}-serialized-ucd-call") {
                    response = utils.deploy(ucdUrl, ucdToken, requestJson)
                }

                echo("Deploy response: ${response}")
                def deploymentRequestId = UDClient.mapFromJson(response)['requestId']
                waitForDeploymentToFinish(ucdUrl, ucdToken, deploymentRequestId)
            }
        }
    }

    try {
        parallel deployStreams
    } catch (error) {
        echo "Error running parallel snapshot deployment: ${error.message}"
        echo "Continuing despite failures."
        throw error
    }

    printSummary(snapshotName, environment)
}

private createDeploySnapshotJson(deployContext, processName, snapshotName) {

    def appName = deployContext.platforms.ucd.app_name
    def onlyChanged = deployContext.platforms.ucd.only_changed
    def environment = deployContext.release.environment

    def jsonMap = [application          : appName,
                   applicationProcess   : processName,
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

    error("UCD deployment failed, result: ${result}")
}

private printSummary(snapshotName, environment) {
    echo """
|=======================================================================|
|***********************************************************************| 
|                                                                       |
| SNAPSHOT ${snapshotName} IS NOW DEPLOYED TO ${environment}            |  
|                                                                       |
|***********************************************************************|
|=======================================================================|
"""
}

return this
