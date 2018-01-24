/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers
import com.lbg.workflow.sandbox.CWABuildHandlers
import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.sandbox.JobStats

def call(String application, handlers, String configuration) {
  this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', 120)
}

def call(String application, handlers, String configuration, String notifyList) {
  this.call(application, handlers, configuration, notifyList, 120)
}

def call(String application, handlers, String configuration, Integer timeoutInMinutes) {
  this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', timeoutInMinutes)
}

def call(String application,
  handlers,
  String configuration,
  String notifyList,
  Integer timeoutInMinutes) {

  try {
    echo "Start BuildPipelineHawk for ${configuration} / ${notifyList} / ${timeoutInMinutes}"

    timeout(timeoutInMinutes) {
      this.callHandler(application, handlers, configuration)
      currentBuild.result = 'SUCCESS'
    }

  } catch(error) {
    currentBuild.result = 'FAILURE'
    echo "BuildPipelineHawk caught exception [" + error.getMessage() + "]."
    throw error

  } finally {
    if(notifyList?.trim()) {
      emailNotify { to = notifyList }
    }
    echo "Finally invoke Splunk after " + currentBuild.result
    def jobStats = new JobStats()
    jobStats.toSplunk(env.BUILD_TAG, env.BUILD_URL, "jenkins-read-all", currentBuild.result, "")
  }

}

def callHandler(String application, handlers, String configuration) {
  def targetCommit
  def branch
  def localBranchName
  BuildContext context
  BuildHandlers initializer
  Utils utils = new Utils()

  node ('framework') {

    echo "Checking out from scm.."
    checkout scm


    // Only stash the pipeline folder if it exists
    def pipelineFolder = new File( 'pipelines/' )
    if( pipelineFolder.exists() ) {
      echo "Stashing.."
      stash  name: 'pipelines', includes: 'pipelines/**'
    }

    // env.BRANCH_NAME is only available in multibranch pipeline jobs
    // to support scheduled pipeline jobs, we define and use local branch name
    branch = env.BRANCH_NAME
    if (branch == null) {
        branch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
    }
    echo "Determine commit id.."
    targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

    echo "Cleanup.."
    context = new BuildContext(application, readFile(configuration))
    step([$class: 'WsCleanup', notFailBuild: true])

}

  if (isPatchsetBranch(branch) ) {

    echo "PatchsetWorkflow for ${targetCommit}.."
    hawkPatchsetWorkflow(context, handlers, targetCommit)

  } else if (isFeatureBranch(branch) ) {

    branch1 =  "ft-" + utils.friendlyName(branch, 20)
    echo "FeatureWorkFlow for ${branch1}.."
    hawkFeatureWorkflow(context, handlers, branch1)

  } else if (isIntegrationBranch(branch) ) {

    branch1 = utils.friendlyName(branch, 40)
    echo "IntegrationWorkFlow for ${branch1}.."
    hawkIntegrationWorkflow(context, handlers, branch1)

  } else {
    error "No known git-workflow rule for branch called ${branch}"
  }
  echo "End BuildPipelineHawk for ${branch}"
}

return this;
