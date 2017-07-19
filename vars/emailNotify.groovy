/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.global.EmailManager

import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.global.GlobalUtils

def call(Closure body){
	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def recipients = config.to?: 'lloydscjtdevops@sapient.com'
	def path = config.path?: 'display/redirect'
	def result = config.stage?: currentBuild.result
	node('master'){
		try{
			timeout(5){

				def utils = new Utils()
				def globalUtils = new GlobalUtils()
				def emailSender = new EmailManager()
				def imagefile = 'j2-result-' + env.BUILD_NUMBER + '.png'

				def headline = globalUtils.urlDecode(
						"J2:${env.JOB_NAME}:${env.BUILD_NUMBER}-> ${result}")

				utils.snapshotRelativeURL(imagefile, path)
				echo "TRYING: Email Notification to ${recipients}"
				emailSender.sendImage(env.WORKSPACE +'/'+ imagefile,
						recipients,
						headline,
						env.BUILD_URL)
				echo "SUCCESS: Email Notification to ${recipients}"
			}
		} catch(error) {
			   echo error.message
			   echo "ERROR: Email Notification to ${recipients}. Not fatal, Onwards!"
		} finally {
			   step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

return this;
