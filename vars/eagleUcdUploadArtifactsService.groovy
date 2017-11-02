import com.lbg.workflow.sandbox.deploy.ServiceWrapper
import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(deployContext) {
    echo "Running UCD Upload"
    for (def service in deployContext.services) {
        uploadComponentArtifacts(service, deployContext)
    }
}

def call(service, deployContext) {
    uploadComponentArtifacts(service, deployContext)
}

private uploadComponentArtifacts(service, deployContext) {

    if (!service.deploy) {
        echo "Skipping service: ${service.name}"
        return
    }

    def wrappedService = new ServiceWrapper(service)
    def ucdUtils = new UtilsUCD()

    def componentName = wrappedService.componentName()
    def componentVersion = wrappedService.componentVersion()
    def componentArtifactsFolder = "${deployContext.platforms.ucd.work_dir}/${componentName.replaceAll("\\s", "")}"
    def ucdUrl = deployContext.platforms.ucd.ucd_url
    def ucdCredentialsTokenName = deployContext.platforms.ucd.credentials

    def versionAlreadyUploaded = withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        def getVersionsResponse = ucdUtils.getComponentVersions(ucdUrl, ucdToken, componentName)
        echo("Version Information: " + getVersionsResponse.toString())
        ucdUtils.componentVersionAlreadyUploaded(getVersionsResponse, componentName, componentVersion)
    }

    if (versionAlreadyUploaded)
        return

    echo("Uploading component ${componentName} version ${componentVersion} to UCD")

    // prepare folder
    sh """if [ -e ${componentArtifactsFolder} ]; then rm -rfv ${componentArtifactsFolder}; fi; """
    sh """mkdir -p "${componentArtifactsFolder}" """

    // download artifacts
    downloadArtifact(componentArtifactsFolder, wrappedService.artifactSaveAsName(), wrappedService.nexusUrl())

    // create version & add artifacts
    withUcdClientAndCredentials(ucdUrl, ucdCredentialsTokenName) { ucdToken ->
        def createVersionResponse = ucdUtils.createComponentVersion(ucdUrl, ucdToken, componentName, componentVersion)
        echo("Create Version Output: ${createVersionResponse}")

        def addVersionResponse = ucdUtils.addFilesToVersion(ucdUrl, ucdToken, componentName, componentVersion, componentArtifactsFolder)
        echo("Add Version Output: ${addVersionResponse}")
    }

}

/**
 * Downloads artifact from nexus url and saves it in a folder
 */
private void downloadArtifact(folder, artifactSaveAsName, nexusUrl) {
    def wgetCmd = "wget --no-check-certificate --quiet --output-document=${folder}/${artifactSaveAsName}"
    sh """${wgetCmd} ${nexusUrl} """
}
