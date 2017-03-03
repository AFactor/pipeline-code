/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.global.EmailManager

import com.lbg.workflow.sandbox.Utils

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def recipients = config.to?: 'lloydscjtdevops@sapient.com'
	try{
		node('master') {
			def utils = new Utils()
			def emailSender = new EmailManager()
			def imagefile = 'j2-' + env.JOB_BASE_NAME + env.BUILD_NUMBER + '.png'
			def branchName = env.BRANCH_NAME?: ''
			def headline = "J2:${env.JOB_BASE_NAME}:${branchName}:${env.BUILD_NUMBER}-> ${currentBuild.result}"

			utils.snapshotStatus(imagefile)
			emailSender.sendImage(	env.WORKSPACE + '/'+ imagefile,
					recipients,
					headline)
			echo "Sent Email Notification to ${recipients}"
		}
	}catch(error) {
		echo "Email Notification failed"
	}
}
return this;
