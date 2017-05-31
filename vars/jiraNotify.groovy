/*
 * Author: Surjit Bains <sbains@sapient.com>
 */

import com.lbg.workflow.global.JiraPublisher

import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.global.GlobalUtils

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	node('master') {
		try{
			timeout(5){

				def utils = new Utils()
				def globalUtils = new GlobalUtils()
				def jiraPublisher = new JiraPublisher()

				def headline = globalUtils.urlDecode(
						"J2:${env.JOB_NAME}:${env.BUILD_NUMBER}-> ${currentBuild.result}")
						//
						// GIT_CHANGE_LOG=$(git log --pretty="$GIT_LOG_FORMAT" $LAST_SUCCESS_REV..HEAD)
						// echo "GIT_CHANGE_LOG=$(git log --no-merges --pretty="$GIT_LOG_FORMAT" $LAST_SUCCESS_REV..HEAD | while
						// read line
						// 	do
						// 		echo $line\\n | tr -d n
						// 	done)"
						//sprint10/gt/CSRV-366
						fullBranch= ${env.BRANCH_NAME}

						int index = fullBranch.lastIndexOf("/");
						String jiraKey = fullBranch.substring(index + 1);

						if(jiraKey != null && !jiraKey.isEmpty()){
							jiraPublisher.addJiraComment(jiraKey,"")
							echo "SUCCESS: Jira Notification submitted "
						}else
						{
							echo "FAILED: Jira, Couldn't find index key "
						}

			}
		}catch(error) {
			echo error.message
			echo "ERROR: Jira Notification failed. Not fatal, Onwards!"
		} finally {
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}
return this;
