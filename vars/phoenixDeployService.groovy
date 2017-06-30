import com.lbg.workflow.sandbox.deploy.phoenix.Components
import com.lbg.workflow.ucd.UDClient

def call(service, deployContext, jobType) {
    println "Deploying service ${service.name}"
    println "Deployment method ${deployContext.deployment.type}"

    // check the deployment type before configuring how to handle the deployment
    switch (deployContext.deployment.type) {
        case 'ucd':
            timeout(deployContext.deployment.timeout) {
                node(deployContext.label) {
                    withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
                        withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
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
                                    cwaSetComponentPaths(service)
                                    break
                                case 'api':
                                    apiArtifactPath(service)
                                    apiExtract(service, deployContext)
                                    break
                                case 'salsa':
                                    apiArtifactPath(service)
                                    apiExtract(service, deployContext)
                                    break
                                case 'mca':
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
    def verScript = "find . -name version.txt -exec cat '{}' \\; -quit"
    srvBin = service.runtime.binary
    sh """mkdir -p ${distsPath} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xvzf ${artifactName} -C ${distsPath} """
    def revision = sh(returnStdout:true, script: verScript).trim().split('-').last().trim()
    phoenixLogger(3, "Revision :: ${revision}", 'star')
    srvBin.revision = revision
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
        def nameComp = srvBin.artifactName.split(/\./)
        srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
        def lastDash = nameComp[0].split('-').last()
        if (lastDash =~ /\w{7}/) {
            srvBin.version = srvBin.artifactName.split(nameComp[0])[-2]
        } else {
            srvBin.version = lastDash
        }
        def verDash = '-' + srvBin.version
        srvBin.name = nameComp[0].split(verDash)[0]
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.artifactName
        phoenixLogger(3, "Paths : Updated Configuration ::  ${service}", 'dash' )
    }
}

private void apiArtifactPath(service) {
    srvBin = service.runtime.binary
    def verScript = "find . -name version.txt -exec cat '{}' \\; -quit"
    if (!srvBin.artifact) {
        def revision = sh(returnStdout:true, script: verScript).trim().split('-').last().trim()
        srvBin.revision = revision
        def nameComp = srvBin.artifactName.split(/\./)
        srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
        srvBin.version = nameComp[0].split('-').last()
        def verDash = '-' + srvBin.version
        srvBin.name = nameComp[0].split(verDash)[0]
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.version + "/" + srvBin.artifactName
    }

}

private def cwaSetComponentPaths(service) {
    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def versionPath = "${version}-${revision}"
    for (def comp in service.components) {
        def baseVersion = comp.baseDir + "/" + versionPath
        def baseVerPath = comp.baseDir + "/" + version
        if (comp.baseDir.contains(version)) {
            break
        }
        def baseVerPathScript = "if [ -d ${baseVerPath} ] ; then echo yes ;else echo no; fi"
        def baseVerScript = "if [ -d ${baseVersion} ] ; then echo yes ;else echo no; fi"
        def bvpExists = sh(returnStdout:true, script: baseVerPathScript).trim()
        def bvExists = sh(returnStdout:true, script: baseVerScript).trim()

        phoenixLogger (4, "baseVerPath: ${baseVerPath} :: Exists: ${bvpExists}", 'star')
        phoenixLogger (4, "baseVersion: ${baseVersion} :: Exists: ${bvExists}", 'star')
        if (bvpExists == 'yes') {
            comp.baseDir = baseVerPath
        } else if (bvExists == 'yes') {
            comp.baseDir = baseVersion
        }
    }
    phoenixLogger(3, "Components : Updated Configuration ::  ${service}", 'dash' )
}

return this;
