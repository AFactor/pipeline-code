/**
 * Builds a deployment pipeline job
 *
 * @param gitRepo Git source of the deploy repo
 * @param deployJob name of the deploy job
 * @param jobConfiguration job configuration file to be used
 */
def call(gitRepo, deployJob, jobConfiguration){
  this.call(gitRepo, 'jenkins-code-reader', deployJob, jobConfiguration)
}

/**
 * Builds a deployment pipeline job
 *
 * @param gitRepo Git source of the deploy repo
 * @param gitCredential Jenkins credentials ID
 * @param deployJob name of the deploy job
 * @param jobConfiguration job configuration file to be used
 */
def call(gitRepo, gitCredential, deployJob, jobConfiguration){
    echo "add deploy jobs"
    jobDsl scriptText: 	"""
		pipelineJob("${deployJob}") {
			definition {
				cpsScm {
                      scm {
                        git {
                          remote {
                            url('${gitRepo}')
                            credentials('${gitCredential}')
                          }
                          branch('master')
                        }
                      }
                      scriptPath('Jenkinsfile')
                }
			}
		}
	"""
}

return this;
