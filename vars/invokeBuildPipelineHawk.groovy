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
	}finally {
		if(notifyList?.trim()){
			emailNotify { to = notifyList }
		}
		def jobStats = new JobStats()
		jobStats.toSplunk(env.BUILD_TAG, env.BUILD_URL, "jenkins-read-all", currentBuild.result)
	}
}

def callHandler(String application, handlers, String configuration) {
	def targetCommit
	BuildContext context
	BuildHandlers initializer
	Utils  utils = new Utils()

	node ('framework'){
		checkout scm
		targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

		stash  name: 'pipelines', includes: 'pipelines/**'

		context = new BuildContext(application, readFile(configuration))
		step([$class: 'WsCleanup', notFailBuild: true])
	}


	if (env.BRANCH_NAME =~ /^patchset\/[0-9]*\/[0-9]*\/[0-9]*/ )  {
		hawkPatchsetWorkflow(context, handlers, targetCommit)
	} else if (env.BRANCH_NAME =~ /^sprint[0-9]+\/.+$/ || env.BRANCH_NAME =~ /^epic\/.+$/ ) {
		hawkFeatureWorkflow( context, handlers, "ft-" + utils.friendlyName(env.BRANCH_NAME, 20))
	} else if (env.BRANCH_NAME =~ /^release-prod.*$/ || env.BRANCH_NAME =~ /^releases\/.*$/ )  {
		hawkIntegrationWorkflow( context, handlers, utils.friendlyName(env.BRANCH_NAME, 40))
	} else if (env.BRANCH_NAME =~ /^master$/ )  {
		hawkIntegrationWorkflow( context, handlers, 'master')
	}  else if (env.BRANCH_NAME =~ /^hotfixes.*$/ )  {
		hawkIntegrationWorkflow( context, handlers, utils.friendlyName(env.BRANCH_NAME, 40))
	}  else if (env.BRANCH_NAME =~ /^develop$/ )  {
		hawkIntegrationWorkflow( context, handlers, 'develop')
	} else {
		echo "We dont know how to build this branch. Stopping"
	}
	echo "End deployment cycle"
}

return this;
