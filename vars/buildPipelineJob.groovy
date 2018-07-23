/**
 * Builds a pipeline job
 *
 * @param gitRepo Git source of the deploy repo
 * @param gitCredential Jenkins credentials ID
 * @param deployJob name of the deploy job
 */
def call( Map map = [:], String jobName ) {
    def jobDefinition = """cps { 
        script('') 
        sandbox() 
    }"""
    if ( map.script != null ) {
        scriptFile = 'pipelineBuilder.groovy'
        writeFile text: "${map.script}", file: "${scriptFile}"

        jobDefinition = """
        cps {
                script(readFileFromWorkspace('${scriptFile}'))
                sandbox()
            }
        """
    }
    if ( map.scriptFile != null ) {
        jobDefinition = """
        cps {
                script(readFileFromWorkspace('${map.scriptFile}'))
                sandbox()
            }
        """
    }
    if ( map.scmFile != null ) {
        gitBranch = map.gitBranch ?: 'master'
        gitCredential = map.gitCredential ?: 'jenkins-code-reader'
        jobDefinition = """
        cpsScm {
                scm {
                    git {
                        remote {
                            url('${map.gitRepo}')
                            credentials('${gitCredential}')
                        }
                        branch('${gitBranch}')
                        extensions {
                            localBranch('${gitBranch}')
                        }
                    }
                }
                scriptPath('${map.scmFile}')
            }
        """
    }
    def numToKeep = map.numToKeep ?: '10'
    def triggers = map.triggers ?: ''

    echo "Add pipeline job: ${jobName}"

    def dslScript =	"""
		pipelineJob("${jobName}") {
            wrappers { preBuildCleanup() }

			definition {
                ${jobDefinition}
			}
            logRotator {
                numToKeep(${numToKeep})
            }
            triggers {
                ${triggers}
            }
		}
	"""
    echo "${dslScript}"
    jobDsl scriptText: "${dslScript}"
}

return this;
