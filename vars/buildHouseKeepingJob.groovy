/**
 *  methods that facilitate the creation of scheduled jobs
 *  only used for jenkins-stats creation currently
 */


// For compatiblity with previous function definition
def call(gitBranch, gitRepo, jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron) {
  this.call(gitBranch, gitRepo, jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron, "")
}

/**
 * Builds a scheduled pipeline job specific for Jenkins-stats
 *
 * @param gitBranch Git branch of the project
 * @param gitRepo Git source of the project
 * @param jobFolder Folder that job should reside in
 * @param jobPath Name and path of the freestyle job
 * @param restrictHost, host or pattern to restrict build to, if blank will not restrict
 * @param shellScript shell script to run
 * @param scriptArguments shell script paramaters to be supplied
 * @param scheduleCron cron schedule to actually run the job
 * @param extraWrappers additional wrappers, e.g. to load credentials

 */
def call(gitBranch, gitRepo, jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron, extraWrappers) {
    this.call(gitBranch, gitRepo, 'jenkins-code-reader', jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron, extraWrappers)
}

/**
 * Builds a scheduled pipeline job specific for Jenkins-stats
 *
 * @param gitBranch Git branch of the project
 * @param gitRepo Git source of the project
 * @param gitCredentials Jenkins credentialID for access to git
 * @param jobFolder Folder that job should reside in
 * @param jobPath Name and path of the freestyle job
 * @param restrictHost, host or pattern to restrict build to, if blank will not restrict
 * @param shellScript shell script to run
 * @param scriptArguments shell script paramaters to be supplied
 * @param scheduleCron cron schedule to actually run the job
 * @param extraWrappers additional wrappers, e.g. to load credentials

 */
def call(gitBranch, gitRepo, gitCredentials, jobFolder, jobPath, restrictHost, shellScript, scriptArguments, scheduleCron, extraWrappers) {    
    echo "Creating housekeeping cleanup job -> ${jobPath}"

    jobDsl scriptText:  """folder('${jobFolder}') """
    def job =  """job('${jobPath}') {
        wrappers { preBuildCleanup() }
        wrappers { ${extraWrappers} }
        label('${restrictHost}')
        scm {
            git {
              remote {
                url('${gitRepo}')
                credentials('${gitCredentials}')
              }
              branch('${gitBranch}')
              extensions {
                localBranch('${gitBranch}')
              }
            }
        }
        steps {
            shell ('sh ${shellScript} ${scriptArguments}')
        }
        triggers {
            cron('${scheduleCron}')
        }
        logRotator {
            numToKeep(10)
        }
    }
    """
    jobDsl (scriptText: job)
    echo "created job ${jobPath}"
}

return this;
