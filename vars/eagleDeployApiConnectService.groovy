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
        envs["APIC_SERVER"] = deployContext.platforms.apiconnect.server
        envs["APIC_ORG"] = deployContext.platforms.apiconnect.org
        envs.putAll(service?.platforms?.apiconnect ?: [:])
        withCredentials([
                usernamePassword(credentialsId: deployContext.platforms.apiconnect.credentials,
                        passwordVariable: 'APIC_PASS',
                        usernameVariable: 'APIC_USER')
        ]) {
            withEnv(utils.toWithEnv(envs)) {
                try {
                    dir("${env.WORKSPACE}/${service.name}") {
                        def tokens = buildTokens(service, deployContext)
                        if (tokens.size() > 0 && fileExists("urbanCode")) {
                            replaceTokens("urbanCode", tokens)
                            sh("cp -rf urbanCode/* ./  2>/dev/null || : && cp -rf urbanCode/.* ./ 2>/dev/null || :")
                        }

                        sh "mkdir -p pipelines/scripts/"
                        writeFile file: "pipelines/scripts/deploy.sh", text: deployAppScript()
                        sh 'source pipelines/scripts/deploy.sh; deployApp'
                    }
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

private def buildTokens(service, deployContext) {
    return service?.tokens ?: [:]
}

private String deployAppScript() {
    return """
        #!/bin/bash
        source ~/.bashrc
        set -ex
        export HOME=\$WORKSPACE
        unset http_proxy https_proxy
        export PATH=\$APIC_PATH:\$PATH  
        env      
        function deployApp() {
            cd \$deployable
            rm -rf .apiconnect/
            rm -rf ~/.apiconnect/
            apic config:set log-level=debug --disable-analytics --accept-license
            apic login --server \$APIC_SERVER --username \$APIC_USER --password \$APIC_PASS            
            npm \$NPM_RUN_TASK
            apic catalogs --server \$APIC_SERVER --organization \$APIC_ORG
            apic logout --server \$APIC_SERVER
        }
    """
}
