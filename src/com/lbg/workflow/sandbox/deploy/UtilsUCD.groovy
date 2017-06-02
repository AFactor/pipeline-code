package com.lbg.workflow.sandbox.deploy

//@org.jenkinsci.plugins.workflow.libs.Library('workflowlib-ucd-global@master')
import com.lbg.workflow.ucd.UDClient
import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def ucdUploadNew(service, deployContext, ucdToken, baseDir, name) {
    println "********************"
    println " Running UCD Upload "
    println "********************"

    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def ucdUrl = deployContext.deployment.ucd_url
    def versionPath = "${version}-${revision}"
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def nameSet = "-name ${version}"
    def versionSet = "-version ${version}"
    def baseSet = "-base ${baseDir}"
    def valueSet = "-value ${versionPath}"
    def linkName = "-linkName \'Jenkins Build upload\'"
    def linkSet = "-link \"\$env.BUILD_URL\""
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"

    echo "Base Dir: ${baseDir} :: Name: ${name}"
    sh """ ${ucdCmd} getComponentVersions ${componentSet}|| get_version=\$? && \\
             if [ \"\$get_version\" != \"\" ]; then \\
                   ${ucdCmd} createVersion ${componentSet} ${nameSet} || if [ "\$?" != "" ]; then exit 1; fi ; \\
                   ${ucdCmd} addVersionFiles ${componentSet} ${versionSet} ${baseSet} || if [ "\$?" != "" ]; then exit 1; fi ; \\
                   ${ucdCmd} setVersionProperty ${componentSet} ${versionSet} -name versionPath ${valueSet} || if [ "\$?" != "" ]; then exit 1; fi ; \\
                   ${ucdCmd} addVersionLink ${componentSet} ${versionSet} ${linkName} ${linkSet} || if [ "\$?" != "" ]; then exit 1; fi ; \\
               else \\
                   echo "Skipping Component : ${name} :: It already exists " ;\\
               fi """
}

@NonCPS
def ucdGenJSON(service, deployContext, ucdToken) {
    println "********************"
    println " Running ucdGenJSON "
    println "********************"
    def jsonGenerator = UDClient.ucdGenJSON(deployContext, service, ucdToken, env)
    return jsonGenerator
}
@NonCPS
def ucdDeploy(deployContext, ucdToken, jsonFile) {
    println "**************************"
    println " Running UCD Deploy Shell "
    println "**************************"
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def ucdScript = "${ucdCmd} requestApplicationProcess - ${jsonFile} "
    def request = sh(returnStdout:true, script: ucdScript).trim()
    return request
}

@NonCPS
def ucdStatus(deployContext, ucdToken, requestId) {
    println "********************"
    println " Running UCD Status "
    println "********************"
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def ucdScript = "${ucdCmd} getApplicationProcessRequestStatus -request ${requestId} "
    def request = sh(returnStdout:true, script: ucdScript).trim()
    return request
}

@NonCPS
def ucdResult(deployContext, ucdToken, requestId) {
    println "********************"
    println " Running UCD Result "
    println "********************"
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def ucdScript = "${ucdCmd} getApplicationProcessExecution -request ${requestId} "
    sh "${ucdScript}"
}


