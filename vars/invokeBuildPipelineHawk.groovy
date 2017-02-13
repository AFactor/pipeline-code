/*
 * Author: Abhay Chrungoo <abhay@ziraffe.io>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.BuildHandlers
import com.lbg.workflow.sandbox.Utils

def call(String application, BuildHandlers handlers, String configuration){
	def targetCommit
	BuildContext context
	BuildHandlers initializer
	Utils  utils = new Utils()

	node (){
		checkout scm
		targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

		stash  name: 'pipelines', includes: 'pipelines/**'

		context = new BuildContext(application, readFile(configuration))

	}


	if (env.BRANCH_NAME =~ /^patchset\/[0-9]*\/[0-9]*\/[0-9]*/ )  {
		hawkPatchsetWorkflow(context, handlers, targetCommit)
	} else if (env.BRANCH_NAME =~ /^sprint[0-9]+\/.+$/ ) {
		hawkFeatureWorkflow( context, handlers, "ft-" + utils.friendlyName(env.BRANCH_NAME, 20))
	} else if (env.BRANCH_NAME =~ /^release-prod.*$/ )  {
		hawkIntegrationWorkflow( context, handlers, 'release-prod')
	} else if (env.BRANCH_NAME =~ /^master$/ )  {
		hawkIntegrationWorkflow( context, handlers, 'master')
	}  else if (env.BRANCH_NAME =~ /^hotfixes.*$/ )  {
		hawkIntegrationWorkflow( context, handlers, 'hotfixes')
	} else {
		echo "We dont know how to build this branch. Stopping"
	}
	echo "End deployment cycle"
}

return this;