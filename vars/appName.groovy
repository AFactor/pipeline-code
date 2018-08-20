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

def call(BuildContext context, String targetBranch, Integer custom_app_length){
   int size = context.application.trim().size()
   if (size > custom_app_length) {
       newAppName=context.application.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-")[0..custom_app_length].replaceAll("^-+","").replaceAll("-+\$","")
   } else {
       newAppName=context.application.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-").replaceAll("^-+","").replaceAll("-+\$","")
   }
 
   newTargetBranch=targetBranch.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-").replaceAll("^-+","").replaceAll("-+\$","")
   return "j2-${newAppName}-${newTargetBranch}"
}
 
 
def call(String application, String targetBranch, Integer custom_app_length){
 
   int size = application.trim().size()
   if (size > custom_app_length) {
       newAppName=application.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-")[0..custom_app_length].replaceAll("^-+","").replaceAll("-+\$","")
   } else {
       newAppName=application.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-").replaceAll("^-+","").replaceAll("-+\$","")
   }
 
   newTargetBranch=targetBranch.trim().replaceAll("\\.","-").replaceAll("_","-").replaceAll("-+","-").replaceAll("^-+","").replaceAll("-+\$","")
   return "j2-${newAppName}-${newTargetBranch}"
}
