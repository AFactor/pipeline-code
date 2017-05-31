def call(service, deployContext) {
    println "Deploying service ${service.name}"
    println "Deployment method ${deployContext.deployment.type}"

    // check the deployment type before configuring how to handle the deployment
    switch (deployContext.deployment.type) {
        case 'ucd':
            createArtifactPath(service)
            node(deployContext.label) {
                withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
                    withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin']) {
                        checkout scm
                        println "******************************"
                        println " Downloading UCD Dependencies "
                        println "******************************"
                        def ucdUrl = deployContext.deployment.ucd_url
                        def wgetCmd = 'wget --no-check-certificate --quiet'
                        sh """${wgetCmd} ${ucdUrl}/tools/udclient.zip ; \\
                                  unzip -o udclient.zip """
                        // check service type to work out best extraction method
                        switch (service.type) {
                            case 'cwa':
                                cwaExtract(service, deployContext)
                                break
                            default:
                                bluemixExtract(service)
                                break
                        }
                        // deploy using ucd functions
                        phoenixDeployUCDService(service, deployContext, ucdToken)
                    }
                }
            }
            break
        case 'bluemix':
            node {
                checkout scm
                println "******************************************"
                println " Download BlueMix Deployment Dependencies "
                println "******************************************"
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
            println "************************************"
            println " Error: No Deployment Type provided "
            println "************************************"
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

private void bluemixExtract(service) {
    def artifact = service.runtime.binary.artifact
    def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
    def wgetCmd = 'wget --no-check-certificate --quiet'
    echo "download artifact ${artifact}"
    sh """mkdir -p ${service.name} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xf ${artifactName} -C ${service.name}"""
}

private void createArtifactPath(service) {
    srvBin = service.runtime.binary
    if (!srvBin.artifact) {
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + "." + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.artifactName
    }
}

return this;
