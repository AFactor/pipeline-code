
def call(service, deployContext, jobType) {
    println "Deploying service ${service.name}"
    println "Deployment method ${deployContext.deployment.type}"

    // check the deployment type before configuring how to handle the deployment
    switch (deployContext.deployment.type) {
        case 'ucd':
            node(deployContext.label) {
                withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
                    withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin']) {
                        checkout scm
                        phoenixLogger(3, "Downloading UCD Deployment", 'dash')
                        def ucdUrl = deployContext.deployment.ucd_url
                        def wgetCmd = 'wget --no-check-certificate --quiet'
                        sh """${wgetCmd} ${ucdUrl}/tools/udclient.zip ; \\
                                  unzip -o udclient.zip """
                        // check service type to work out best extraction method
                        switch (service.type) {
                            case 'cwa':
                                cwaArtifactPath(service)
                                cwaExtract(service, deployContext)
                                break
                            case 'api':
                                apiArtifactPath(service)
                                apiExtract(service, deployContext)
                                break
                            default:
                                bluemixExtract(service)
                                break
                        }
                        // deploy using ucd functions
                        if (jobType == 'deploy') {
                            phoenixDeployUCDService(service, deployContext, ucdToken)
                        }
                        if (jobType == 'upload') {
                            phoenixUploadUCDService(service, deployContext, ucdToken)
                        }
                    }
                }
            }
            break
        case 'bluemix':
            node {
                checkout scm
                phoenixLogger(3, "Download BlueMix Deployment Dependencies", 'star')
                // check service type to work out best extraction method
                switch (service.type) {
                    case 'bluemix':
                        bluemixExtract(service)
                        break
                    default:
                        bluemixExtract(service)
                        break
                }
                // deploy using bluemix functions
                eagleDeployBluemixService(service, deployContext)
            }
            break
        default:
            phoenixLogger(1, "No Deployment Type provided", 'star')
            throw new Exception("Error: No Deployment Type provided")
            return null
    }
}

private void cwaExtract(service, deployContext) {
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def distsPath = deployContext.deployment.work_dir
    def wgetCmd = 'wget --no-check-certificate --quiet'
    sh """mkdir -p ${distsPath} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xzf ${artifactName} -C ${distsPath} """
    echo "All Done - Extraction Complete"
}

private void apiExtract(service, deployContext) {
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def distsPath = deployContext.deployment.work_dir
    def workDir = distsPath + "/" + service.name
    def extractPath = workDir + "/Expanded"
    def wgetCmd = 'wget --no-check-certificate --quiet'
    sh """if [ -e dist ]; then rm -rfv dist; fi; \\
          mkdir -p ${extractPath} && \\
          ${wgetCmd} ${artifact} && \\
          unzip -o "${artifactName}" -d "${workDir}" && \\
          mv ${workDir}/*.war ${extractPath} && \\
          for war in `ls -1 ${extractPath}/*.war`; do \\
          unzip -o \$war -d ${workDir}/\$(basename \$war) ;\\
          rm -f \$war ;\\
          done ; \\
          rm -rfv $extractPath"""
    echo "All Done - Extraction Complete"

}

private void bluemixExtract(service) {
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    def wgetCmd = 'wget --no-check-certificate --quiet'
    echo "download artifact ${artifact}"
    sh """mkdir -p ${service.name} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xf ${artifactName} -C ${service.name}"""
}

private void cwaArtifactPath(service) {
    srvBin = service.runtime.binary
    if (!srvBin.artifact) {
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + "." + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.artifactName
    }
}

private void apiArtifactPath(service) {
    srvBin = service.runtime.binary
    if (!srvBin.artifact) {
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + "." + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.version + "/" + srvBin.artifactName
    }

}

return this;
