import com.lbg.workflow.sandbox.deploy.DeployContextBuilder
import groovy.json.JsonOutput

def call() {
    def deployContext
    node() {
        checkout scm
        def _envs = JOB_NAME.substring(JOB_NAME.lastIndexOf('release')).split("/")
        def environment = env.RELEASE_ENVIRONMENT ?: _envs[1]
        def target = env.RELEASE_TARGET ?: _envs[2].substring(_envs[2].lastIndexOf('-') + 1)

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
        echo "Deploy Context: <${JsonOutput.toJson(deployContext)}>"
        milestone(label: 'Validate')
    }
    deployContext
}


def call(configuration) {
    def deployContext
    node() {
        try {
            stage('Initialise') {
                checkout scm
                milestone(label: 'Initialised')
            }

            stage('Validate') {
                def builder = new DeployContextBuilder(configuration)
                deployContext = builder.deployContext
                validate(deployContext)

            }
        } catch (error) {
            echo "Invalid deployment configuration $error.message"
            throw error
        }
        echo "Deploy Context: <${JsonOutput.toJson(deployContext)}>"
        milestone(label: 'Validate')
    }
    deployContext
}

private def validate(deployContext) {
    isValid("journey", deployContext.release.journey)
    if (deployContext.release.environment != "test"
            && deployContext.release.environment != "testm"
            && deployContext.release.environment != "oipe"
            && deployContext.release.environment != "dev"
            && deployContext.release.environment != "DEV"
            && deployContext.release.environment != "TEST"
            && deployContext.release.environment != "SIT") {
        for (def service : deployContext.services) {
            def artifact = service.runtime.binary.artifact
            if (artifact.contains("-rc.") || artifact.contains("-de.")) {
                error "Invalid artifact configuration for service ${service.name}. Artifacts from master branches are not allowed"
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
