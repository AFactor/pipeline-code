/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.global.EmailManager

import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.global.GlobalUtils

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def recipients = config.to?: 'lloydscjtdevops@sapient.com'
	try{
		timeout(5){
			node('master') {
				def utils = new Utils()
				def globalUtils = new GlobalUtils()
				def emailSender = new EmailManager()
				def imagefile = 'j2-result-' + env.BUILD_NUMBER + '.png'

				def headline = globalUtils.urlDecode(
						"J2:${env.JOB_NAME}:${env.BUILD_NUMBER}-> ${currentBuild.result}")

				utils.snapshotStatus(imagefile)
				echo "TRYING: Email Notification to ${recipients}"
				emailSender.sendImage(	env.WORKSPACE + '/'+ imagefile,
						recipients,
						headline,
						blueoceanJobURL())
				echo "SUCCESS: Email Notification to ${recipients}"
			}
		}
	}catch(error) {
		echo error.message
		echo "ERROR: Email Notification to ${recipients}. Not fatal, Onwards!"
	}
}
return this;
