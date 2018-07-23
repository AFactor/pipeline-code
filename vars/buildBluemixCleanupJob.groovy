// Stale bluemix cleanup job

def call(jobPath, organization_name, space_name, threshold, scheaduleCron) {
    this.call(jobPath, organization_name, 'bluemix-global-deployer', space_name, threshold, scheaduleCron)
}

def call(jobPath, organization_name, bmCredentialId, space_name, threshold, scheaduleCron) {

    echo "Creating bluemix cleanup job for ORG -> ${organization_name} using ${bmCredentialId}"

    def jobFolder = jobPath.substring(0, jobPath.length() - jobPath.split('/').last().length() - 1)
    def restrictHost = "master"
    def gitBranch = "master"
    def gitRepo = "ssh://gerrit.sandbox.extranet.group:29418/devops-jenkins2-jobspec"
    def gitCredentials = "jenkins-code-reader"
    def shellScript = "include-scripts/bluemix-cleanup.sh"
    def scheduleCron = "0 5 * * 1-5"
    def scriptArguments = "${organization_name} ${space_name} ${threshold}"
    // def extraWrappers = "credentialsBinding {usernamePassword('BM_USER','BM_PASS','${bmCredentialId}')}"
    // buildHouseKeepingJob(gitBranch, gitRepo, jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron, extraWrappers)

    echo "Creating housekeeping cleanup job -> ${jobPath}"

    jobDsl scriptText:  """folder('${jobFolder}') """

    def pipelineScript = """pipeline {
    agent none
    options {
        ansiColor('xterm')
        skipDefaultCheckout()
        timestamps()
        disableConcurrentBuilds()
    }
    stages{
        stage('Initialise') {
            agent { label '${restrictHost}' }
            steps {
                checkout([\$class: 'GitSCM', userRemoteConfigs: [[url: '${gitRepo}', credentialsId: '${gitCredentials}']]])
            }
        }
        stage('Bluemix Cleanup') {
            agent { label '${restrictHost}' }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: '${bmCredentialId}',
                                passwordVariable: 'BM_PASS',
                                usernameVariable: 'BM_USER')]) {
                    sh "sh ${shellScript} ${scriptArguments}"
                }
            }
        }
    }
}
"""

    buildPipelineJob(jobPath, script:"${pipelineScript}", triggers:"cron('${scheduleCron}')")
}

return this;
