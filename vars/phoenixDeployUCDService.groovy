import com.lbg.workflow.sandbox.deploy.phoenix.Status
import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(service, deployContext, ucdToken) {
    phoenixLogger(4, "UCD Deployment", 'star')
    def utils = new UtilsUCD()
    def outputPath = "${env.WORKSPACE}/ucd_config.json"

    jsonData = utils.ucdGenJSON(service, deployContext, ucdToken)
    sh "echo \'${jsonData}\' > ${outputPath}"

    String result = utils.ucdDeploy(deployContext, ucdToken, outputPath)
    def jsonResult = new Status(result)
    def requestId = jsonResult.requestId
    phoenixLogger(3, "Request ID: ${requestId}", 'dash')
    String requestStatus = utils.ucdStatus(deployContext, ucdToken, requestId)
    def jsonStatus = new Status(requestStatus)
    def status = jsonStatus.status
    phoenixLogger(3, "Status: ${status}", 'dash')
    int statChecker = 0
    while (status != "CLOSED") {
        requestStatus = utils.ucdStatus(deployContext, ucdToken, requestId)
        jsonStatus = new Status(requestStatus)
        status = jsonStatus.status
        phoenixLogger(3, "Status Currently :: ${status}", 'dash')
        if (status == "ERROR" || status == "FAILED") {
            break
        }
        // 15 sec per run - 120 counts should be 30 minutes
        // if it cant deploy in 30 minutes - there must be an issue
        if (statChecker > 120) {
            status == "TIMEOUT"
            break
        }
        statChecker ++
        sleep(15)
    }
    if (status == "CLOSED") {
        utils.ucdResult(deployContext, ucdToken, requestId)
    } else {
        throw new Exception ("Error Deployment :: ${status}")
    }
}

