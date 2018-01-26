package com.lbg.workflow.sandbox
import com.lbg.workflow.global.EmailManager
import com.lbg.workflow.global.GlobalUtils

def build(String targetBranch, context, String pathToBuildScript) {
  // Building 'artifacts' stash with a given script
  node(){
    unit = load(pathToBuildScript)
    unit.runTest(targetBranch, context)
  }
}

def uploadVeracode(String targetBranch, context) {
  def veracodeCredentials = context.config.veracode.credentials ?: 'veracode-creds'
  def veracodeID = context.config.veracode.id
  def artifacts = context.config.veracode.artifacts ?: 'artifacts'
    withCredentials([
      usernamePassword(credentialsId: veracodeCredentials,
      passwordVariable: 'API_PASSWORD',
      usernameVariable: 'API_USERNAME')
    ]){
    withEnv([
       "APP_ID=${veracodeID}",
    ]) {
        checkout scm
        dir ("artifacts") {
            unstash artifacts
        }
        try {
            sh "mkdir -p ${WORKSPACE}/pipelines/scripts/"
            writeFile file: "${WORKSPACE}/pipelines/scripts/veracode_upload.sh", text: deployUploadScript()
            writeFile file: "${WORKSPACE}/pipelines/scripts/veracode_functions.sh", text: deployFunctionsScript()
            sh "chmod +x ${WORKSPACE}/pipelines/scripts/veracode_*"
            sh "${WORKSPACE}/pipelines/scripts/veracode_upload.sh ${WORKSPACE}/artifacts"
        } catch (error) {
            echo "FAILURE: veracode failed"
            echo error.message
            throw error
        } finally {
            step([$class: 'WsCleanup', notFailBuild: true])
          }
        }
      }
}

def downloadVeracode(String targetBranch, context) {
  def veracodeCredentials = context.config.veracode.credentials ?: 'veracode-creds'
  def notificationList = context.config.veracode.notificationList ?: 'LloydsCJTDevOps@sapient.com'
  def veracodeID =  context.config.veracode.id

    withCredentials([
      usernamePassword(credentialsId: veracodeCredentials,
      passwordVariable: 'API_PASSWORD',
      usernameVariable: 'API_USERNAME')
    ]){
    withEnv([
       "APP_ID=${veracodeID}",
       "WORKSPACE=${env.WORKSPACE}"
    ]) {
        checkout scm
        try {
            sh "mkdir -p ${WORKSPACE}/pipelines/scripts/"
            writeFile file: "${WORKSPACE}/pipelines/scripts/veracode_download.sh", text: deployDownloadScript()
            writeFile file: "${WORKSPACE}/pipelines/scripts/veracode_functions.sh", text: deployFunctionsScript()
            sh "chmod +x ${WORKSPACE}/pipelines/scripts/veracode_*"
            sh "${WORKSPACE}/pipelines/scripts/veracode_download.sh"
            stash name: "veracodereport", includes: 'veracodeResults/*.xml'

            archiveArtifacts allowEmptyArchive: true,
                    artifacts: 'veracodeResults/',
                    fingerprint: true,
                    onlyIfSuccessful: false

            sh "zip -rq VeracodeReport.zip veracodeResults"

            stash name: "veracodezip", includes: 'VeracodeReport.zip'

        } catch (error) {
            echo "FAILURE: veracode failed"
            echo error.message
            throw error
        } finally {
            //WsCleanup
            step([$class: 'WsCleanup', notFailBuild: true])
        }
      }
    }
}

def emailVeracode(String targetBranch, context) {
  def notificationList = context.config.veracode.notificationList ?: 'LloydsCJTDevOps@sapient.com'

  def emailSender = new EmailManager()
  def imagefile = 'VeracodeReport.zip'
  def headline  = 'Veracode reports'
  def appName = appName(context.application, targetBranch)
  unstash "veracodezip"
  sh 'ls -la'

  echo "TRYING: Email Notification to ${notificationList}"

  emailSender.sendImage(env.WORKSPACE +'/'+ imagefile,
    notificationList, headline + appName, env.BUILD_URL)

  echo "SUCCESS: Email Notification to ${notificationList}"
}

def emailVeracodeFail(String targetBranch, context) {
  def notificationList = context.config.veracode.notificationList ?: 'LloydsCJTDevOps@sapient.com'

  def emailSender = new EmailManager()
  def headline  = GlobalUtils.urlDecode("J2:${env.JOB_NAME}:${env.BUILD_NUMBER} Veracode reports FAILED".toString())
  def appName = appName(context.application, targetBranch)
  echo "TRYING: Email Notification to ${notificationList}"
  messageBody = "${env.JOB_NAME}:${env.BUILD_NUMBER} Veracode reports FAILED, please see jenkins log for more info"
  emailSender.sendHtml(notificationList, headline + appName, messageBody)

  echo "SUCCESS: Email Notification to ${notificationList}"
}

def publishSplunk(String targetBranch, String epoch, context){
                       def appname = appName(context.application, targetBranch)
                       def splunkReportDir = "${context.config.splunk.reportdir}"
                       echo "PUBLISH: ${this.name()} ${appname} reports to Splunk"
                       dir ("j2/${appname}") {
                           unstash "veracodereport"
                           sh 'ls -lR'
                           splunkPublisher.SCP('veracodeResults/*.xml',
                                               "${splunkReportDir}")
                       }
}

private def deployUploadScript() {
        libraryResource "com/lbg/workflow/sandbox/veracode/veracode_upload.sh"
}

private def deployDownloadScript() {
        libraryResource "com/lbg/workflow/sandbox/veracode/veracode_download.sh"
}

private def deployFunctionsScript() {
        libraryResource "com/lbg/workflow/sandbox/veracode/veracode_functions.sh"
}

String name() {
    return "Veracode"
}

return this;
