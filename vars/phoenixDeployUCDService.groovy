import com.lbg.workflow.sandbox.deploy.phoenix.Status
import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.sandbox.deploy.phoenix.Components

def call(service, deployContext, ucdToken) {
    println "************************"
    println " Running UCD Deployment "
    println "************************"
    def outputPath = "${env.WORKSPACE}/ucd_config.json"
    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    //def versionPath = "${version}-${revision}"
    def utils = new UtilsUCD()

    jsonData = utils.ucdGenJSON(service, deployContext, ucdToken)
    sh "echo \'${jsonData}\' > ${outputPath}"


    for (Object componentObject : service.components) {
        Components comp = componentObject
        def baseDir = "./" + comp.baseDir
        /*
            This code is currently unused - need to be sure this will actually work in all scenarios
            def baseVerPath =  comp.baseDir + "/" version
            def baseVersion =  comp.baseDir + "/" versionPath
            def bvpExists = utils.folderChecker(baseVerPath)
            def bvExists = utils.folderChecker(baseVersion)
            if (bvpExists == true) {
                baseDir = "./" + baseVerPath
            } else if (bvExists == true) {
                baseDir = "./" + baseVersion
            }
        */

        def name = comp.name
        utils.ucdUploadNew(service, deployContext, ucdToken, baseDir, name)
    }
    String result = utils.ucdDeploy(deployContext, ucdToken, outputPath)
    def jsonResult = new Status(result)
    def requestId = jsonResult.requestId
    echo "Request ID == ${requestId}"
    String requestStatus = utils.ucdStatus(deployContext, ucdToken, requestId)
    def jsonStatus = new Status(requestStatus)
    def status = jsonStatus.status
    echo "Status == ${status}"
    int statChecker = 0
    while (status != "CLOSED") {
        requestStatus = utils.ucdStatus(deployContext, ucdToken, requestId)
        jsonStatus = new Status(requestStatus)
        status = jsonStatus.status
        echo "Status Currently :: ${status}"
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

