/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.global.GlobalUtils

String call(String suffix){
	def globalUtils = new GlobalUtils()
	def jobUrl = env.JENKINS_URL
	def urlTokens = env.BUILD_URL.replace(env.JENKINS_URL, '').split('/')

	for (String token: urlTokens){
		jobUrl += globalUtils.urlEncode(token)
		jobUrl += '/'
	}

	return jobUrl + suffix
}
