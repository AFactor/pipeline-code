import com.lbg.workflow.sandbox.deploy.DeployContextBuilder

def call() {
    def deployContext
    node() {
        checkout scm

        def _envs = "$env.JOB_NAME".split("/")
        def environment = _envs[1]
        def target = _envs[2].substring(_envs[2].lastIndexOf('-') + 1)

        def release
        def services
        def servicesTokens
        def platforms
        def platformsTokens

        try {
            stage('Initialise') {
                release = "${environment}/${target}/release/release.json"
                if (release == null || !fileExists(release)) {
                    error 'release config missing'
                }
                services = "${environment}/${target}/services/services.json"
                if (services == null || !fileExists(services)) {
                    error 'services config missing'
                }
                servicesTokens = "${environment}/${target}/services/tokens.json"
                if (servicesTokens == null || !fileExists(servicesTokens)) {
                    error 'services config missing'
                }
                platforms = "${environment}/${target}/platforms/platforms.json"
                if (platforms == null || !fileExists(platforms)) {
                    error 'platforms config missing'
                }
                platformsTokens = "${environment}/${target}/platforms/tokens.json"
                if (platformsTokens == null || !fileExists(platformsTokens)) {
                    error 'platforms config missing'
                }
            }
            milestone(label: 'Initialised')

            stage('Validate') {
                def builder = new DeployContextBuilder(readFile(release), readFile(services), readFile(servicesTokens), readFile(platforms), readFile(platformsTokens))
                deployContext = builder.deployContext
                validate(deployContext)

            }
        } catch (error) {
            echo "Invalid deployment configuration $error.message"
            throw error
        }
        echo "Deploy Context " + deployContext.toString()
        milestone(label: 'Validate')
    }
    deployContext
}


private def validate(deployContext) {
    isValid("journey", deployContext.release.journey)
    if (deployContext.release.environment != "test" && deployContext.release.environment != "testm" && deployContext.release.environment != "pink") {
        for (def service : deployContext.services) {
            def artifact = service.runtime.binary.artifact
            if (artifact.contains("-rc.") || artifact.contains("-de.")) {
                error "Invalid artifact configuration for service ${service.name}. Only artifacts from bugfix/* branches are allowed"
            }
        }
    }
    // TODO enforce stricter service and platform validation
}


private def isValid(name, value) {
    if (!value) {
        error "$name config must be defined"
    }
}
return this