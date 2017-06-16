import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.sandbox.deploy.phoenix.Components

def call(service, deployContext, ucdToken) {
    phoenixLogger(4, "UCD Upload ", "star")

    switch(service.type){
        case 'cwa':
            cwaUpload(service,deployContext,ucdToken)
            break
        case 'api':
            apiUpload(service,deployContext,ucdToken)
            break
        default:
            phoenixLogger(1, "No Service Type definition found for ${service.name}", 'star')
            throw new Exception("Error: No Service Type provided")
            break
    }
}

private def cwaUpload(service, deployContext, ucdToken) {
    def utils = new UtilsUCD()
    for (Object componentObject : service.components) {
        Components comp = componentObject
        def baseDir = "./" + comp.baseDir
        def name = comp.name
        def date = new Date().format("ddMMyyyyHHMM", TimeZone.getTimeZone('UTC'))
        phoenixLogger(5,"Base Dir: ${baseDir} :: Name: ${name}", "dash")
        def getVersion = utils.ucdComponentVersion(deployContext, ucdToken, name)
        echo "Current Version information: ${getVersion}"
        def createVersion = utils.cwaCreateVersion(service, deployContext, ucdToken, name, date)
        phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

        def addVersion = utils.cwaAddVersion(service, deployContext, ucdToken, baseDir, name, date)
        phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')

        def setVersion = utils.ucdSetVersionProperty(service, deployContext, ucdToken, name, date)
        phoenixLogger(3, "Set Version Property Output: ${setVersion}", 'dash')

        def addLink = utils.ucdAddVersionLink(service, deployContext, ucdToken, name, date)
        phoenixLogger(3, "Add Version Link Output: ${addLink}", 'dash')
    }
}

private def apiUpload(service, deployContext, ucdToken) {
    def utils = new UtilsUCD()
    def date = new Date().format("ddMMyyyyHHMM", TimeZone.getTimeZone('UTC'))
    for (Object componentObject : service.components) {
        Components comp = componentObject
        def baseDir = "./" + comp.baseDir
        def name = comp.name
        phoenixLogger(5, "Base Dir: ${baseDir} :: Name: ${name}", "dash")

        def getVersion = utils.ucdComponentVersion(deployContext, ucdToken, name)
        echo "Current Version information: ${getVersion}"
        def createVersion = utils.apiCreateVersion(service, deployContext, ucdToken, name, date)
        phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

        def addVersion = utils.apiAddVersion(service, deployContext, ucdToken, comp.baseDir, name, date)
        phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')
    }
}

/*
    if (getVersion == "") {
    } else {
        phoenixLogger(3,"Skipping Component: ${name} :: Already Deployed", "star")
    }
 */
