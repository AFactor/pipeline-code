import com.lbg.workflow.sandbox.deploy.ManifestBuilder
import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(service, deployContext) {
    if (service.buildpack == "Node.js") {
        // build manifest
        echo "build service manifest"
        def appName = "${deployContext.journey}-${service.name}-${deployContext.env}"
        def manifest = new ManifestBuilder().build(appName, service, deployContext)
        sh "mkdir -p ${service.name}/pipelines/conf"
        writeFile file: "${service.name}/pipelines/conf/manifest.yml", text: manifest

        // deploy service
        echo "deploy service"
        def utils = new UtilsBluemix()
        def bluemixEnvs = utils.buildServiceBluemixEnv(service.bluemix, deployContext.bluemix)
        bluemixEnvs["deployable"] = "${env.WORKSPACE}/${service.name}"
        bluemixEnvs["APP"] = "${deployContext.journey}-${service.name}-${deployContext.env}"
        withCredentials([
                usernamePassword(credentialsId: deployContext.bluemix.credentials,
                        passwordVariable: 'BM_PASS',
                        usernameVariable: 'BM_USER')
        ]) {
            withEnv(utils.toWithEnv(bluemixEnvs)) {
                try {
                    sh "mkdir -p pipelines/scripts/"
                    writeFile file: "pipelines/scripts/deploy.sh", text: deployAppScript()
                    sh 'source pipelines/scripts/deploy.sh; deployApp'
                } catch (error) {
                    echo "Deployment failed"
                    throw error
                } finally {
                    step([$class: 'WsCleanup', notFailBuild: true])
                }
            }
        }
    } else {
        error "Skipping service deployment, no implementation for buildpack $service.buildpack"
    }
}

private String deployAppScript() {
    return """
        #!/bin/bash
        set -ex
        
        function deployApp() {
            cd \$deployable
            cp pipelines/conf/manifest.yml  manifest.yml
            cat manifest.yml
            cf logout
            cf login -a \$BM_API -u \$BM_USER -p \$BM_PASS -o \$BM_ORG -s \$BM_ENV
            cf delete \${APP} -f -r || echo "Failed to delete application."
            cf push -f manifest.yml
        }
    """
}