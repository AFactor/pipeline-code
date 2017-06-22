/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers
import com.lbg.workflow.sandbox.CWABuildHandlers
import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.sandbox.JobStats

def call(String application, handlers, String configuration){
	this.call(application, handlers, configuration,	'lloydscjtdevops@sapient.com', 120 )
}

def call(String application, handlers, String configuration,String notifyList){
	this.call(application, handlers, configuration,	notifyList,	120 )
}

def call(String application, handlers, String configuration, Integer timeoutInMinutes){
	this.call(application, handlers, configuration,	'lloydscjtdevops@sapient.com', timeoutInMinutes)
}

def call(String application,
		handlers,
		String configuration,
		String notifyList,
		Integer timeoutInMinutes){
	try {
		timeout(timeoutInMinutes){
			this.callHandler(application, handlers, configuration)
			currentBuild.result = 'SUCCESS'
		}
	} catch(error) {
		currentBuild.result = 'FAILURE'
		throw error
	} finally {
		if(notifyList?.trim()){
			emailNotify { to = notifyList }
		}
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

	node ('framework'){
		checkout scm

    localBranchName = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
		targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

		stash  name: 'pipelines', includes: 'pipelines/**'

		context = new BuildContext(application, readFile(configuration))
		step([$class: 'WsCleanup', notFailBuild: true])
	}

  // env.BRANCH_NAME is only available in multibranch pipeline jobs
  // to support scheduled pipeline jobs, we define and use local branch name
  branch = env.BRANCH_NAME
  if (branch == null) {
    branch = localBranchName
  }

	if (branch =~ /^patchset\/[0-9]*\/[0-9]*\/[0-9]*/ ) {
		  hawkPatchsetWorkflow(context, handlers, targetCommit)
	} else if (branch =~ /^sprint[0-9]+\/.+$/ || branch =~ /^epic\/.+$/ ) {
		  hawkFeatureWorkflow(context, handlers, "ft-" + utils.friendlyName(branch, 20))
	} else if (branch =~ /^release-prod.*$/ || branch =~ /^releases\/.*$/ ) {
		  hawkIntegrationWorkflow(context, handlers, utils.friendlyName(branch, 40))
	} else if (branch =~ /^master$/ ) {
		  hawkIntegrationWorkflow(context, handlers, 'master')
	} else if (branch =~ /^hotfixes.*$/ ) {
		  hawkIntegrationWorkflow(context, handlers, utils.friendlyName(branch, 40))
	} else if (branch =~ /^develop$/ ) {
		  hawkIntegrationWorkflow(context, handlers, 'develop')
	} else {
      echo "We dont know how to build this branch. Stopping."
	}
	echo "End deployment cycle"
}

return this;
