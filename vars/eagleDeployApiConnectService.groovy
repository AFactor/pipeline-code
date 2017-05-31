import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(service, deployContext) {
    if (service.buildpack == "Node.js") {
        // deploy service
        echo "deploy service"
        def utils = new UtilsBluemix()
        def envs = [:]
        envs["deployable"] = "${env.WORKSPACE}/${service.name}"
        envs["APIC_SERVER"] = deployContext.apiconnect.server
        envs["APIC_ORG"] = deployContext.apiconnect.org
        if (service.env != null) {
            for (e in service.env) {
                envs[e.key] = e.value
            }
        }
        def task = service.env["NPM_SETUP_TASK"]
        def envConfig = getEnvironmentConfig()
        withCredentials([
                usernamePassword(credentialsId: deployContext.apiconnect.credentials,
                        passwordVariable: 'APIC_PASS',
                        usernameVariable: 'APIC_USER')
        ]) {
            withEnv(utils.toWithEnv(envs)) {
                try {
                    if (fileExists(envConfig)) {
                        sh "cp -rf $envConfig ${env.WORKSPACE}/${service.name}/$envConfig"
                    }
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
        error "Skipping service deployment, no implementation for buildpack $service.buildpack"
    }
}

private String getEnvironmentConfig() {
    def jobName = "${env.JOB_NAME}"
    def targetEnv = jobName.substring(jobName.lastIndexOf('-') + 1)
    return "config/${targetEnv}.json"
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
            apic config:set log-level=debug --disable-analytics --accept-license
            apic login --server \$APIC_SERVER --username \$APIC_USER --password \$APIC_PASS            
            npm $task
            apic catalogs --server \$APIC_SERVER --organization \$APIC_ORG
            apic logout --server \$APIC_SERVER
        }
    """
}