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
    println "Request Status: ${requestStatus}"
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
        // 15 sec per run - 360 counts should be 1 hour 30 minutes
        // if it cant deploy in 1 hour 30 minutes - there must be an issue
        if (statChecker > 360) {
            status == "TIMEOUT"
            break
        }
        statChecker ++
        sleep(15)
    }
    if (status == "CLOSED") {
        utils.ucdResult(deployContext, ucdToken, requestId)
    } else {
        //throw new Exception ("Error Deployment :: ${status}")
        phoenixLogger(1, "Status :: ${status}", 'star')
    }
}

