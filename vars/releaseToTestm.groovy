def call(String packageVersion, context) {
    lock(inversePrecedence: true, quantity: 1, resource: "release-tracker-testm-update") {
        def nexus = "https://nexus.sandbox.extranet.group/nexus/content/repositories"
        def group = context.config.nexus.groupId
        def artifact = context.config.nexus.artifactId
        def repo	= 'releases'
        def groupPath = group.replace('.','/')
        def releaseJob = context.config.release.job ?: 'ob-release/sandbox/autodeploy-testm'

        stash includes: 'pipelines/conf/release/**/*', name: 'config'

        String artifactUrl = "${nexus}/${repo}/${groupPath}/${artifact}/${packageVersion}/${artifact}-${packageVersion}.tar.gz"

        ws {
            git branch: 'feature/auto-promote',
                url: context.config.release.repoUrl,
                credentialsId: context.config.release.credentials,
                poll: false,
                changelog: false

            def serviceKey
            dir('temp') {
                unstash 'config'
                localService = readJSON(file:'pipelines/conf/release/testm/services/services.json')
                serviceKey = localService["name"]
                localServiceTokens = readJSON(file:'pipelines/conf/release/testm/services/tokens.json')
                localPlatformTokens = readJSON(file:'pipelines/conf/release/testm/platforms/tokens.json')
                deleteDir()
            }

            def environmentPath = 'sandbox/testm'

            localService["runtime"]["binary"]["artifact"] = artifactUrl
            setEntry("${environmentPath}/services/services.json", serviceKey, localService);
            setEntry("${environmentPath}/platforms/tokens.json", serviceKey, localPlatformTokens)
            setEntry("${environmentPath}/services/tokens.json", serviceKey, localServiceTokens)

            try {
                sshagent([context.config.release.credentials]) {
                    sh """
                        git config user.name jenkins
                        git config user.email alex.knowles@sapient.com

                        git diff --quiet && git diff --staged --quiet && echo "no changes" ||  \
                        {
                            git commit -am 'auto deployment of ${serviceKey}\n\n\n(using ${artifact}-${packageVersion})' &&  \
                            git push -u origin feature/auto-promote
                        }
                    """
                }
                if(! context.config.release?.nofire) {
                    build job: releaseJob, wait: false
                }
            } finally {
                sh "git config --remove-section user"
                cleanWs notFailBuild: true
            }
        }
    }
}

def setEntry (String file, String key, Map value) {
    Map json = readJSON(file: file)
    def index = json["services"].findIndexOf { service ->
        service["name"] == key
    }

    if (index < 0) {
        if (!json["services"]) {
            json["services"] = []
        }
        json["services"].add(value)
    } else {
        json["services"][index] = value
    }

    // and sort
    json["services"].sort { a,b ->
        a["name"] <=> b["name"]
    }

    writeJSON(file: file, json: json, pretty: 4)

}