import com.lbg.workflow.sandbox.deploy.phoenix.Components
import com.lbg.workflow.sandbox.deploy.phoenix.Service
import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.deploy.UtilsUCD

private def apiBddTests (deployContext, stage) {
    for (Object serviceObject : deployContext.services) {
        Service service = serviceObject
        if (service.deploy) {
            node(deployContext.label) {
                switch(service.type) {
                    case 'api':
                        switch (stage) {
                            case 'pre-BDD':
                                if (deployContext.tests.pre_bdd) {
                                    preBddCheck(deployContext, service)
                                }
                                break
                            case 'post-BDD':
                                if (deployContext.tests.post_bdd) {
                                    postBddCheck(deployContext, service)
                                }
                                break
                            default:
                                phoenixLogger(2, "Stage: ${stage} :: Not Found :: Skipping", 'dash')
                                return null
                                break
                        }
                        break
                    case 'mca':
                        switch (stage) {
                            case 'pre-BDD':
                                if (deployContext.tests.pre_bdd) {
                                    preBddCheck(deployContext, service)
                                }
                                break
                            case 'post-BDD':
                                if (deployContext.tests.post_bdd) {
                                    postBddCheck(deployContext, service)
                                }
                                break
                            default:
                                phoenixLogger(2, "Stage: ${stage} :: Not Found :: Skipping", 'dash')
                                return null
                                break
                        }
                        break
                    case 'salsa':
                        switch (stage) {
                            case 'pre-BDD':
                                if (deployContext.tests.pre_bdd) {
                                    preBddCheck(deployContext, service)
                                }
                                break
                            case 'post-BDD':
                                if (deployContext.tests.post_bdd) {
                                    postBddCheck(deployContext, service)
                                }
                                break
                            default:
                                phoenixLogger(2, "Stage: ${stage} :: Not Found :: Skipping", 'dash')
                                return null
                                break
                        }
                        break
                    case 'cwa':
                        switch (stage) {
                            case 'pre-BDD':
                                if (deployContext.tests.pre_bdd) {
                                    preBddCheck(deployContext, service)
                                }
                                break
                            case 'post-BDD':
                                if (deployContext.tests.post_bdd) {
                                    postBddCheck(deployContext, service)
                                }
                                break
                            default:
                                phoenixLogger(2, "Stage: ${stage} :: Not Found :: Skipping", 'dash')
                                return null
                                break
                        }
                        break
                    default:
                        phoenixLogger(1, " Error: No Service Type provided  ", 'star')
                        throw new Exception("Error: No Service Type provided")
                        break
                }
            }
        } else {
            phoenixLogger(3, "Deployment Is Disabled Skipping ${stage}", 'star')
        }
    }

}

private def preBddCheck(deployContext, service) {
    withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
        withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
            def utils = new UtilsUCD()
            def gitRepo = deployContext.tests.repo
            def gitUrl = "http://gerrit.sandbox.extranet.group/${gitRepo}"
            def name = ''
            for (Object componentObject : service.components) {
                Components comp = componentObject
                name = comp.name
            }
            def ucdUrl = deployContext.deployment.ucd_url
            def wgetCmd = 'wget --no-check-certificate --quiet'
            sh """${wgetCmd} ${ucdUrl}/tools/udclient.zip ; \\
                                  unzip -o udclient.zip """
            def getVersion = utils.ucdComponentVersion(deployContext, ucdToken, name)
            def uploadedVersion = utils.getLatestVersionUploadJson(getVersion, service, name)
            def gitRef = uploadedVersion.trim().split('-').last().trim()
            def branch = deployContext.tests.branch
            def credentials = deployContext.tests.credentials
            phoenixLogger(4, "Git Reference: ${gitRef} :: Branch: ${branch} :: credentials ${credentials}", 'dash')
            bddCall(credentials, gitRepo, gitUrl, gitRef, branch)
        }
    }
}

private def postBddCheck(deployContext, service) {
    withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
        def gitRepo = deployContext.tests.repo
        def gitUrl = "http://gerrit.sandbox.extranet.group/${gitRepo}"
        def gitRef = service.runtime.binary.revision
        def branch = deployContext.tests.branch
        def credentials = deployContext.tests.credentials
        bddCall(credentials, gitRepo, gitUrl, gitRef, branch)
    }
}


private def bddCall (credentials, gitRepo, gitUrl, gitRef, targetBranch) {
    def testDir = "${env.WORKSPACE}"
    def filePath = testDir + "/pipelines/tests/bdd.groovy"
    def bddConfig = testDir + "/pipelines/conf/job-configuration.json"
    BuildContext context
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: "${gitRef}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class: 'CleanCheckout']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "${credentials}", url: "${gitUrl}"]]
    ])

    try {
        context = new BuildContext(readFile(bddConfig))
        load(filePath).runTest(targetBranch, context)
        milestone(label: "BDD Test Done")
    } catch (error) {
        phoenixLogger(1, "api BDD Test Stage Failure $error.message", 'star')
        currentBuild.result = 'FAILURE'
        phoenixNotifyStage().notify(deployContext)
        throw error
    }
}


private def deployTest(deployContext) {
    switch(deployContext.deployment.type) {
        case 'ucd':
            echo "Skipping Tests for Now"
            break
        case 'bluemix':
            try {
                eagleDeployTester(deployContext)
            } catch (error) {
                phoenixLogger(1, "Test Stage Failure $error.message", 'star')
                currentBuild.result = 'FAILURE'
                phoenixNotifyStage().notify(deployContext)
                throw error
            } finally {
            }
            break
        default:
            phoenixLogger(1, "No Deployment Type provided", 'star')
            currentBuild.result = 'FAILURE'
            phoenixNotifyStage().notify(deployContext)
            throw new Exception("Error: No Deployment Type provided")
            break
    }
}

private def envCheck(deployContext) {
    return null
}

return this;
