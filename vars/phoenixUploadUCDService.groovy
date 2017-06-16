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
        phoenixLogger(5,"Base Dir: ${baseDir} :: Name: ${name}", "dash")
        def getVersion = utils.ucdComponentVersion(deployContext, ucdToken, name)
        echo "Current Version information: ${getVersion}"
        if (getVersion == "") {
            def createVersion = utils.cwaCreateVersion(service, deployContext, ucdToken, name)
            phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')
            /*
                if (createVersion == "") {
                    phoenixLogger(1, "Could Not Create UCD Version", 'star')
                    throw new Exception("Upload Error")
                }
            */
            def addVersion = utils.cwaAddVersion(service, deployContext, ucdToken, baseDir, name)
            phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')
            /*
                if (addVersion == "") {
                    phoenixLogger(1, "Could Not Add UCD Version Files", 'star')
                    throw new Exception("Upload Error")
                }
            */
            def setVersion = utils.ucdSetVersionProperty(service, deployContext, ucdToken, name)
            phoenixLogger(3, "Set Version Property Output: ${setVersion}", 'dash')
            /*
                if (setVersion == "") {
                    phoenixLogger(1, "Could Not Set UCD Version Property Information", 'star')
                    throw new Exception("UCD Version Property Error")
                }
            */
            def addLink = utils.ucdAddVersionLink(service, deployContext, ucdToken, name)
            phoenixLogger(3, "Add Version Link Output: ${addLink}", 'dash')
            /*
                if (addLink == "") {
                    phoenixLogger(1, "Could Not Add UCD Version Link", 'star')
                    throw new Exception("UCD Version Link Error")
                }
            */
        } else {
            phoenixLogger(3,"Skipping Component: ${name} :: Already Deployed", "star")
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
        echo "Current Version information: ${getVersion}"
        //if (getVersion == "") {
        //}
        def createVersion = utils.apiCreateVersion(service, deployContext, ucdToken, name, date)
        phoenixLogger(3, "Create Version Output: ${createVersion}", 'dash')

        def addVersion = utils.apiAddVersion(service, deployContext, ucdToken, comp.baseDir, name, date)
        phoenixLogger(3, "Add Version Output: ${addVersion}", 'dash')
        /*
            if (createVersion != "") {
                phoenixLogger(1, "Could Not Create UCD Version", 'star')
                throw new Exception("Upload Error")
            }
            if (addVersion != "") {
                phoenixLogger(1, "Could Not Add UCD Version Files", 'star')
                throw new Exception("Upload Error")
            }
        */
    }
}

