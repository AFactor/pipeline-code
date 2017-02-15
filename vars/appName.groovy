/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.sandbox.BuildContext

def call(String application, String targetBranch){
	return "j2-${application}-${targetBranch}"
}

def call(BuildContext context, String targetBranch){
	return "j2-${context.application}-${targetBranch}"
}