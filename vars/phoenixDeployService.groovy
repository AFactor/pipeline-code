import com.lbg.workflow.sandbox.deploy.UtilsUCD

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
                            if (deployContext.hasUserInputStep()) {
                                def snapshot = service.snapshot
                                def artifactName = service.runtime.binary.artifactName
                                if(snapshot != null && !snapshot.isEmpty()) {
                                    if (artifactName != null && !artifactName.isEmpty()) {
                                        phoenixLogger(1, "Please select either Artifact or Snapshot, not BOTH", 'star')
                                        currentBuild.result = 'FAILURE'
                                        phoenixNotifyStage().notify(deployContext)
                                    }
                                    if (jobType == 'upload') {
                                        phoenixLogger(3, "Running Snapshot Service :: Skipping Upload Processing", 'dash')
                                        return null
                                    }
                                }
                            }
                            phoenixLogger(3, "Downloading UCD Deployment", 'dash')
                            UtilsUCD utils = new UtilsUCD()
                            utils.install(deployContext)
                            // check service type to work out best extraction method
                            switch (service.type) {
                                case 'cwa':
                                    cwaArtifactPath(service)
                                    cwaExtract(service, deployContext)
                                    cwaSetComponentPaths(service)
                                    break
                                case 'ob-aisp':
                                    obaispArtifactPath(service)
                                    obaispExtract(service, deployContext)
                                    break
                                case 'api':
                                case 'salsa':
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
                    step([$class: 'WsCleanup', notFailBuild: true])
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
                step([$class: 'WsCleanup', notFailBuild: true])
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
    def fullVerScript = "find . -name fullVersion.txt -exec cat '{}' \\; -quit"
    def verScript = "find . -name version.txt -exec cat '{}' \\; -quit"
    def revision
    srvBin = service.runtime.binary

    sh "rm -rf ${distsPath} || true"
    sh """mkdir -p ${distsPath} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xzf ${artifactName} -C ${distsPath} """

    def fullVersion = sh(returnStdout:true, script: fullVerScript).trim()
    if (fullVersion.length() > 0) {
      revision = fullVersion.split('-').last().trim()

      def version = fullVersion.substring(0, fullVersion.length() - revision.length() - 1)
      phoenixLogger(3, "Version :: ${version}", 'star')
      srvBin.version = version
    } else {
      revision = sh(returnStdout:true, script: verScript).trim().split('-').last().trim()
    }

    phoenixLogger(3, "Revision :: ${revision}", 'star')
    srvBin.revision = revision
    echo "All Done - Extraction Complete"
}

private void obaispExtract(service, deployContext) {
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def nameComp = artifactName.split(/\./)
    def buildNum = nameComp[0].split('-')[-2]
    def distsPath = deployContext.deployment.work_dir
    def wgetCmd = 'wget --no-check-certificate --quiet'
    srvBin = service.runtime.binary
    def verScript = "cat ${distsPath}/package.json|grep version|awk -F\\\" '{print \$4}'"
    sh """mkdir -p ${distsPath} && \\
          ${wgetCmd} ${artifact} && \\
          tar -xzf ${artifactName} -C ${distsPath} """
    def version = sh(returnStdout:true, script: verScript).trim()
    phoenixLogger(3, "Version :: ${version}", 'star')
    srvBin.version = version + '.' + buildNum
    sh """rm -rf ${distsPath} && \\
          mkdir -p ${distsPath} && \\
          mv ${artifactName} ${distsPath} """
    echo "All Done - Artifact ready for upload"
}

private void apiExtract(service, deployContext) {
    def artifact = service.runtime.binary.artifact
    def artifactName = service.runtime.binary.artifactName
    def name = service.runtime.binary.name
    def extension = service.runtime.binary.extension
    def distsPath = deployContext.deployment.work_dir
    def workDir = distsPath + "/" + name + "." + extension
    def extractPath = workDir + "/Expanded"
    def wgetCmd = 'wget --no-check-certificate --quiet'
    def verScript = "find . -name version.txt -exec cat '{}' \\; -quit"
    def date = new Date().format("ddMMyyyyHHMM", TimeZone.getTimeZone('UTC'))
    def revision = date
    def gerritRevision = apiGerritRevision(deployContext)
    srvBin = service.runtime.binary
    sh """if [ -e dist ]; then rm -rfv dist; fi; \\
          mkdir -p "${extractPath}" && \\
          ${wgetCmd} ${artifact} """

    sh """unzip -o "${artifactName}" -d "${workDir}" && \\
          mv "${workDir}/"*.war "${extractPath}" && \\
          SAVEIFS=\$IFS && \\
          IFS=\$(echo -en "\\n\\b") &&\\
          for war in `ls -1 "${extractPath}"/*.war`; do \\
          unzip -o "\$war" -d "${workDir}"/\$(basename "\$war") ;\\
          rm -fv "\$war" ;\\
          done ; \\
          IFS=\$SAVEIFS ; \\
          rm -rfv "$extractPath"
        """
    try {
        revision = sh(returnStdout:true, script: verScript).trim().split('-').last().trim()
        phoenixLogger(4, "Revision :: ${revision}", 'dash')
        if (!revision) {
            revision = date
            if(gerritRevision != null && !gerritRevision.isEmpty()) {
                revision = gerritRevision
            }
            phoenixLogger(1, "Revision Not Found :: Revision Now: ${revision}", 'star')
        }
    } catch (error) {
        if(gerritRevision != null && !gerritRevision.isEmpty()) {
            revision = gerritRevision
        }
        phoenixLogger(1, "Revision Not Found :: Revision Now: ${revision}", 'star')
    }
    srvBin.revision = revision
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
        try {
          srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
        } catch (error){
          phoenixLogger(1, "No file extension found in artifact name ${srvBin.artifactName}. Please provide full file name. ABORTING.", 'star')
          throw error
        }
        def lastDash = nameComp[0].split('-').last()
        if (lastDash =~ /\w{7}/) {
            srvBin.version = nameComp[0].split('-')[-2]
        } else {
            srvBin.version = lastDash
        }
        def verDash = '-' + srvBin.version
        srvBin.name = nameComp[0].split(verDash)[0]
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.artifactName
    }
}

private void apiArtifactPath(service) {
    srvBin = service.runtime.binary
    def artifactName = ''
    if (!srvBin.artifact) {
        def nameComp = srvBin.artifactName.split(/\./)
        println ("Name Components == " + nameComp)
        if (nameComp.size() >= 2) {
            if ('tar' == nameComp[-2]) {
                srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
                artifactName = srvBin.artifactName.split("[.]" + srvBin.extension)[0]
            } else {
                srvBin.extension = nameComp.last()
                artifactName = srvBin.artifactName.split("[.]" + srvBin.extension)[0]
            }
        }
        def lastDash = artifactName.split('-').last()
        if (lastDash =~ /^\w{7}$/) {
            srvBin.version = artifactName.split('-')[-2]
        } else {
            srvBin.version = lastDash
        }
        def verDash = '-' + srvBin.version
        srvBin.name = artifactName.split(verDash)[0]
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.version + "/" + srvBin.artifactName
        echo "Artifact Name: ${artifactName} :: Last Dash: ${lastDash} :: Version: ${srvBin.version} :: artifact: ${srvBin.artifact}"
        createScript = "touch version_${srvBin.version}"
        sh(returnStdout:true, script: createScript)
        archiveArtifacts "version_${srvBin.version}"
    }
}

private def apiGerritRevision(deployContext) {
    withCredentials([
            usernameColonPassword(credentialsId: 'GERRIT_HTTP_CREDS', variable: 'GAUTH')
    ]) {
        def gerritRepo = deployContext.tests.repo
        def gerritBranch = 'master'
        def revision = ''
        if (deployContext.tests.branch) {
            gerritBranch = deployContext.tests.branch
        }
        def gerritUrl = "https://gerrit.sandbox.extranet.group/a/projects/${gerritRepo}/branches/${gerritBranch}"
        def gerritScriptRevision = "curl -s -k --digest --user ${GAUTH} ${gerritUrl}|grep revision|awk -F\\\" '{print \$4}'"
        try {
            revision = sh(returnStdout:true, script: gerritScriptRevision).trim()[0..8]
            fullRevision = sh(returnStdout:true, script: gerritScriptRevision).trim()
            println "Full Revision info :: ${fullRevision} :: Actual Revision: ${revision}"
        } catch (error) {
            println "Could not get Revision from Gerrit :: Error: ${error.message}"
            println "Skipping"
        }
        return revision
    }
}

private def cwaSetComponentPaths(service) {
    def version = service.runtime.binary.version

    for (def comp in service.components) {
        def verScript = "find ${comp.baseDir} -name version.txt -exec cat '{}' \\; -quit"
        def revision = sh(returnStdout:true, script: verScript).trim().split('-').last().trim()
        if (revision.length() == 0) {
            revision = service.runtime.binary.revision
        }
        def versionPath = "${version}-${revision}"

        def baseVersion = comp.baseDir + "/" + versionPath
        def baseVerPath = comp.baseDir + "/" + version
        def baseRevPath = comp.baseDir + "/" + revision
        if (comp.baseDir.contains(version)) {
            break
        }

        def baseVerPathScript = "if [ -d ${baseVerPath} ] ; then echo yes ;else echo no; fi"
        def baseRevPathScript = "if [ -d ${baseRevPath} ] ; then echo yes ;else echo no; fi"
        def baseVerScript = "if [ -d ${baseVersion} ] ; then echo yes ;else echo no; fi"
        def bvpExists = sh(returnStdout:true, script: baseVerPathScript).trim()
        def bvExists = sh(returnStdout:true, script: baseVerScript).trim()
        def brExists = sh(returnStdout:true, script: baseRevPathScript).trim()

        phoenixLogger(4, "baseVerPath: ${baseVerPath} :: Exists: ${bvpExists}", 'star')
        phoenixLogger(4, "baseVersion: ${baseVersion} :: Exists: ${bvExists}", 'star')
        phoenixLogger(4, "baseRevPath: ${baseRevPath} :: Exists: ${brExists}", 'star')

        comp.revision = revision
        if (bvpExists == 'yes') {
            comp.baseDir = baseVerPath
            comp.versionPath = version
        } else if (bvExists == 'yes') {
            comp.baseDir = baseVersion
            comp.versionPath = versionPath
        } else if (brExists == 'yes') {
            comp.baseDir = baseRevPath
            comp.versionPath = revision
        }
    }
    phoenixLogger(3, "Components : Updated Configuration ::  ${service}", 'dash' )
}

private void obaispArtifactPath(service) {
    srvBin = service.runtime.binary
    if (!srvBin.artifact) {
        def nameComp = srvBin.artifactName.split(/\./)
        srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
        def lastDash = nameComp[0].split('-').last()
        def buildNum = nameComp[0].split('-')[-2]
        srvBin.revision = lastDash
        def revDash = '-' + buildNum + '-' + srvBin.revision
        srvBin.name = nameComp[0].split(revDash)[0]
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.artifactName
    }
}

return this;
