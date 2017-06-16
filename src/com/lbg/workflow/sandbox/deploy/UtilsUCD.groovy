package com.lbg.workflow.sandbox.deploy

import com.lbg.workflow.ucd.UDClient
import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def ucdComponentVersion(deployContext, ucdToken, name) {
    println "***************************************"
    println " Get UCD Component Version for ${name} "
    println "***************************************"

    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def command = "getComponentVersions ${componentSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def cwaCreateVersion(service, deployContext, ucdToken, name, date) {
    println "********************************"
    println " UCD Create Version for ${name} "
    println "********************************"

    def version = service.runtime.binary.version
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def nameSet = "-name ${version}-${date}"
    def command = "createVersion ${componentSet} ${nameSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def cwaAddVersion(service, deployContext, ucdToken, baseDir, name, date) {
    println "*****************************"
    println " UCD Add Version for ${name} "
    println "*****************************"

    def version = service.runtime.binary.version
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def versionSet = "-version ${version}-${date}"
    def baseSet = "-base ${baseDir}"
    def command = "addVersionFiles ${componentSet} ${versionSet} ${baseSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def apiCreateVersion(service, deployContext, ucdToken, name, date) {
    println "********************************"
    println " UCD Create Version for ${name} "
    println "********************************"

    def version = service.runtime.binary.version
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def nameSet = "-name ${version}-${date}"
    def descSet = "-description ${version}-ear"
    def command = "createVersion ${componentSet} ${nameSet} ${descSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def apiAddVersion(service, deployContext, ucdToken, baseDir, name, date) {
    println "*****************************"
    println " UCD Add Version for ${name} "
    println "*****************************"

    def version = service.runtime.binary.version
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def versionSet = "-version ${version}-${date}"
    def baseSet = "-base ${baseDir}"
    def command = "addVersionFiles ${componentSet} ${versionSet} ${baseSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def ucdSetVersionProperty(service, deployContext, ucdToken, name, date) {
    println "**************************************"
    println " UCD Set Version Property for ${name} "
    println "**************************************"

    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def ucdUrl = deployContext.deployment.ucd_url
    def versionPath = "${version}-${revision}"
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def versionSet = "-version ${version}-${date}"
    def valueSet = "-value ${versionPath}"
    def command = "setVersionProperty ${componentSet} ${versionSet} -name versionPath ${valueSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

@NonCPS
def ucdAddVersionLink(service, deployContext, ucdToken, name, date) {

    println "**********************************"
    println " UCD Add Version Link for ${name} "
    println "**********************************"

    def version = service.runtime.binary.version
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component ${name}"
    def versionSet = "-version ${version}-${date}"
    def linkName = "-linkName \'Jenkins Build upload\'"
    def linkSet = "-link \"\$env.BUILD_URL\""
    def command = "addVersionLink ${componentSet} ${versionSet} ${linkName} ${linkSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
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

/*
    udclient -weburl https://ucd.intranet.group -authtoken $UC_TOKEN  createVersion -component "DigitalMC_sales-pca-api Application" -name "${VERSION}-${vtime}" -description ${VERSION}-ear

    echo "Uploading the artifacts to UrbanCode"
    echo "===================================="

    udclient -weburl https://ucd.intranet.group -authtoken $UC_TOKEN addVersionFiles -component "DigitalMC_sales-pca-api Application" -version "${VERSION}-${vtime}" -base "${URBAN_CODE_UPLOAD_DIR}"

    @NonCPS
    def ucdCompleteUpload(service, deployContext, ucdToken, baseDir, name) {
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
    def ucdApiCreateVersion(service, deployContext, ucdToken, name, date) {
        println "*****************************"
        println " New UCD Version for ${name} "
        println "*****************************"
        def version = service.runtime.binary.version
        def ucdUrl = deployContext.deployment.ucd_url
        def udClient = "./udclient/udclient"
        def componentSet = "-component ${name}"
        def command = "createVersion ${componentSet} ${nameSet} ${descSet}"
        def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

        def request = sh(returnStdout: true, script: ${ucdCmd}).trim()
        return request
    }

    @NonCPS
    def ucdUpload(service, deployContext, ucdToken, baseDir, name, date) {
        println "*********************"
        println " UCD Upload: ${name} "
        println "*********************"
        def version = service.runtime.binary.version
        def ucdUrl = deployContext.deployment.ucd_url
        def udClient = "./udclient/udclient"
        def componentSet = "-component ${name}"
        def baseSet = "-base ${baseDir}"
        def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} addVersionFiles ${componentSet} ${versionSet} ${baseSet}"

        def request = sh(returnStdout: true, script: ${ucdCmd}).trim()
        return request
    }
*/

