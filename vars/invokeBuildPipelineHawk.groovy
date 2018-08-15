import com.lbg.workflow.sandbox.*

def call(String application, handlers, String configuration){
  this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', 120)
}

def call(String application, handlers, String configuration, String notifyList){
  this.call(application, handlers, configuration, notifyList, 120)
}

def call(String application, handlers, String configuration, Integer timeoutInMinutes){
  this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', timeoutInMinutes)
}

// Main invocation
def call(String application, handlers, String configuration, String notifyList, Integer timeoutInMinutes){
  try {
    echo "Start BuildPipelineHawk for ${configuration} / ${notifyList} / ${timeoutInMinutes}"
    timeout(timeoutInMinutes){
      this.callHandler(application, handlers, configuration)
      currentBuild.result = 'SUCCESS'
    }

  } catch(error){
      currentBuild.result = 'FAILURE'
      echo "BuildPipelineHawk caught exception: [" + error.getMessage() + "]."
      throw error
  } finally {
      if (notifyList?.trim()){
        emailNotify { to = notifyList }
      }

      echo "Publishing job stats to splunk after " + currentBuild.result
      def jobStats = new JobStats()
      jobStats.toSplunk(env.BUILD_TAG, env.BUILD_URL, "jenkins-read-all", currentBuild.result, "")
  }

}

def callHandler(String application, handlers, String configuration){
  def targetCommit
  def branch
  def localBranchName
  BuildContext context
  Utils utils = new Utils()
  def loadedHandlers = [:]

  node('framework'){
    echo "Checking out from scm..."
    checkout scm

    // Only stash the pipeline folder if it exists
    if (fileExists("pipelines/")){
      echo "Stashing pipelines folder..."
      stash name: 'pipelines', includes: 'pipelines/**'
    }

    // env.BRANCH_NAME is only available in multibranch pipeline jobs
    // to support scheduled pipeline jobs, we define and use local branch name
    branch = env.BRANCH_NAME
    if (branch == null){
        branch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
    }
    echo "Determine commit id..."
    targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

    // Create BuildContext
    context = new BuildContext(application, readFile(configuration))
    loadedHandlers.unitTests = []
    loadedHandlers.sanityTests = []
    loadedHandlers.integrationTests = []

    // Common initialize stage for all flows
	stage("Initialize"){
		try {
			echo "Loading all handlers..."
			loadedHandlers.builder = loadHandler(handlers.builder)
			loadedHandlers.appDeployer = loadHandler(handlers.deployer)

			for (String test: handlers.getUnitTests()){
				loadedHandlers.unitTests.add(loadHandler(test))
			}
			for (String test: handlers.getStaticAnalysis()){
				loadedHandlers.sanityTests.add(loadHandler(test))
			}
			for (String test: handlers.getIntegrationTests()){
				loadedHandlers.integrationTests.add(loadHandler(test))
			}
		} catch(error){
			print "Encountered error during handlers loading:"
			print error.message
			throw error
		} finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
		milestone (label: 'Ready')
	}
  } // node('framework')

  if (isPatchsetBranch(branch)){
      hawkPatchsetWorkflow(context, loadedHandlers, targetCommit)

  } else if (isFeatureBranch(branch)){
      branch1 = "ft-" + utils.friendlyName(branch, 20)
      hawkFeatureWorkflow(context, loadedHandlers, branch1)

  } else if (isPullRequestBranch(branch)){
      branch1 = utils.friendlyName(branch, 20)
      hawkFeatureWorkflow(context, loadedHandlers, branch1)

  } else if (isIntegrationBranch(branch)){
      branch1 = utils.friendlyName(branch, 40)
      hawkIntegrationWorkflow(context, loadedHandlers, branch1)

  } else {
      error "No known git-workflow rule for branch called ${branch}"
  }

  echo "End BuildPipelineHawk for ${branch}"
}

/* Tries to load handler. If not available, loads default handler instead.
 * Default handler is handler with same name, but located in this library
 * in reseources/com/lbg/workflow/sandbox/handlers. If this fails, uncaught
 * error is propagated upwards, where it will handled.
*/
def loadHandler(String handler){
    print "Loading ${handler}..."
    if (! fileExists(handler)){
        String handlerName = handler.split('/').last()

        print "Using default handler for ${handlerName}..."
        defaultHandler = libraryResource "com/lbg/workflow/sandbox/handlers/${handlerName}"
        writeFile file: handler, text: defaultHandler
    }
    return load(handler)
}

return this;
