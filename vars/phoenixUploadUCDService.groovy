import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.sandbox.deploy.phoenix.Components

def call(service, deployContext, ucdToken) {
    phoenixLogger(4, "UCD Upload ", "star")

    switch(service.type){
        case 'cwa':
            cwaUpload(service,deployContext,ucdToken)
            break
        case 'ob-aisp':
            obaispUpload(service,deployContext,ucdToken)
            break
        case 'api':
        case 'salsa':
        case 'mca':
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

        def versionStatus = utils.getVersionsJson(getVersion, service, comp, name)
        if (!versionStatus) {
            def createVersion = utils.cwaCreateVersion(service, comp, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

            def addVersion = utils.cwaAddVersion(service, comp, deployContext, ucdToken, baseDir, name, date)
            phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')

            def setVersion = utils.ucdSetVersionProperty(service, comp, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Set Version Property Output: ${setVersion}", 'dash')

            def addLink = utils.ucdAddVersionLink(service, comp, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Add Version Link Output: ${addLink}", 'dash')
        }
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
        println ("Version Information: " + getVersion.toString())
        def versionStatus = utils.getVersionsJson(getVersion, service, comp, name)
        if (!versionStatus) {
            def createVersion = utils.apiCreateVersion(service, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

            def addVersion = utils.apiAddVersion(service, deployContext, ucdToken, comp.baseDir, name, date)
            phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')
        }
    }
}


private def obaispUpload(service, deployContext, ucdToken) {
    def utils = new UtilsUCD()
    def date = new Date().format("ddMMyyyyHHMM", TimeZone.getTimeZone('UTC'))
    for (Object componentObject : service.components) {
        Components comp = componentObject
        def baseDir = "./" + comp.baseDir
        def name = comp.name
        phoenixLogger(5, "Base Dir: ${baseDir} :: Name: ${name}", "dash")

        def getVersion = utils.ucdComponentVersion(deployContext, ucdToken, name)
        println ("Version Information: " + getVersion.toString())
        def versionStatus = utils.getVersionsJson(getVersion, service, comp, name)
        if (!versionStatus) {
            def createVersion = utils.apiCreateVersion(service, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

            def addVersion = utils.apiAddVersion(service, deployContext, ucdToken, comp.baseDir, name, date)
            phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')

            def setVersion = utils.ucdSetVersionTarProperty(service, deployContext, ucdToken, name, date)
            phoenixLogger(3, "Set Version Property Output: ${setVersion}", 'dash')
        }
    }
}

