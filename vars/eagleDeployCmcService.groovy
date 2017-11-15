import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(service, deployContext) {
    if (service.type == "Node.js") {
        // fetch  artifact
        def artifact = service.runtime.binary.artifact
        def artifactName = artifact.substring(artifact.lastIndexOf('/') + 1, artifact.length())
        echo "download artifact ${artifact}"
        sh """mkdir -p ${service.name} && \\
                  		wget --quiet ${artifact} && \\
                  		tar -xf ${artifactName} -C ${service.name}""";

        // deploy service
        echo "deploy service"
        def utils = new UtilsBluemix()
        def envs = [:]
        envs["deployable"] = "${env.WORKSPACE}/${service.name}"
        envs["CMC_SERVER"] = deployContext.platforms.cmc.server
        envs["CMC_GATEWAY_CLUSTER_ID"] = deployContext.platforms.cmc.gateway_cluster_id
        if (service.tokens != null) {
            for (e in service.tokens) {
                envs[e.key] = e.value
            }
        }
        def task = service.env["NPM_SETUP_TASK"]
        withCredentials([
                usernamePassword(credentialsId: deployContext.cmc.credentials,
                        passwordVariable: 'CMC_PASS',
                        usernameVariable: 'CMC_USER')
        ]) {
            withEnv(utils.toWithEnv(envs)) {
                try {
                    sh "mkdir -p pipelines/scripts/"
                    writeFile file: "pipelines/scripts/deploy.sh", text: deployAppScript(task)
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
        error "Skipping service deployment, no implementation for buildpack $service.type"
    }
}

private String deployAppScript(String task) {
    return """
        #!/bin/bash
        source ~/.bashrc
        set -ex
        export HOME=\$WORKSPACE
        unset http_proxy https_proxy        
        function deployApp() {
            cd \$deployable
            npm $task
        }
    """
}