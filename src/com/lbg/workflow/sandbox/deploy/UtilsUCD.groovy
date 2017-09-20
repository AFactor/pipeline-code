package com.lbg.workflow.sandbox.deploy

import com.lbg.workflow.ucd.UDClient
import com.lbg.workflow.ucd.UCDVersions
import com.lbg.workflow.ucd.UCDVersionParser
import com.lbg.workflow.ucd.UCDSnapshotParser
import com.lbg.workflow.ucd.UCDEnvironments
import com.lbg.workflow.ucd.UCDEnvironmentParser
import com.cloudbees.groovy.cps.NonCPS

/**
 * All NonCPS methods below return immediately (even inside for loops)
 * This is why there are different methods created, their behaviour is not like normal functions / methods
 * When it comes to @NonCPS methods keep it simple, more info below:
 * https://stackoverflow.com/questions/40196903/why-noncps-is-necessary-when-iterating-through-the-list
 **/

/**
 * @param deployContext
 * @param ucdToken
 * @param name
 * @return
 **/
@NonCPS
def ucdComponentVersion(deployContext, ucdToken, name) {
    println "***************************************"
    println " Get UCD Component Version for ${name} "
    println "***************************************"

    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def command = "getComponentVersions ${componentSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

/**
 * @param deployContext
 * @param ucdToken
 * @param name
 * @return
 **/
String ucdComponentVersionGather(ucdToken, name) {
    println "***************************************"
    println " Get UCD Component Version for ${name} "
    println "***************************************"

    def ucdUrl = 'https://ucd.intranet.group'
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def command = "getComponentVersions ${componentSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"
    install_by_url(ucdUrl)
    def response = sh(returnStdout: true, script: ucdCmd).trim()
    def versionData = "{ \"versions\": " + response + "}"
    def versionParser = new UCDVersionParser(versionData)
    def versions = []
    versions.add('')

    for (def ucdVersion in versionParser.versions) {
        versions.add(ucdVersion.name)
    }
    println(versions)
    return versions.join('\n')
}

String ucdSnapshotVersionGather(ucdToken, name) {
    println "***************************************"
    println " Get UCD Component Version for ${name} "
    println "***************************************"

    def ucdUrl = 'https://ucd.intranet.group'
    def udClient = "./udclient/udclient"
    def applicationSet = "-application '${name}'"
    def command = "getSnapshotsInApplication ${applicationSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"
    install_by_url(ucdUrl)
    def response = sh(returnStdout: true, script: ucdCmd).trim()
    def versionData = "{ \"versions\": " + response + "}"
    def snapshotParser = new UCDVersionParser(versionData)
    def versions = []
    versions.add('')

    for (def snapVersion in snapshotParser.versions) {
        versions.add(snapVersion.name)
    }
    println(versions)
    return versions.join('\n')
}

String ucdApplicationEnvironments(ucdToken, name) {
    println "**********************************************"
    println " Get UCD Application Environments for ${name} "
    println "**********************************************"

    def ucdUrl = 'https://ucd.intranet.group'
    def udClient = "./udclient/udclient"
    def appSet = "-application '${name}'"
    def command = "getEnvironmentsInApplication $appSet"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"
    install_by_url(ucdUrl)
    def response = sh(returnStdout: true, script: ucdCmd).trim()
    def envData = "{ \"environments\": " + response + "}"
    def environmentParser = new UCDEnvironmentParser(envData)
    def envs = []
    envs.add('')

    for (def ucdEnv in environmentParser.environments) {
        envs.add(ucdEnv.name)
    }
    println(envs)
    return envs.join('\n')
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param name
 * @param date
 * @return
 **/
@NonCPS
def cwaCreateVersion(service, deployContext, ucdToken, name, date) {
    println "********************************"
    println " UCD Create Version for ${name} "
    println "********************************"

    def version = service.runtime.binary.version
    def revision = ''
    if (service.runtime.binary.revision.size() > 9) {
        revision = service.runtime.binary.revision[0..9]
    } else {
        revision = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def nameSet = "-name ${version}-${revision}"
    def command = "createVersion ${componentSet} ${nameSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    try{
        def request = sh(returnStdout: true, script: ucdCmd).trim()
        return request
    } catch (error) {
        echo error
        return null
    }
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param baseDir
 * @param name
 * @param date
 * @return
 **/
@NonCPS
def cwaAddVersion(service, deployContext, ucdToken, baseDir, name, date) {
    println "*****************************"
    println " UCD Add Version for ${name} "
    println "*****************************"

    def version = service.runtime.binary.version
    def revision = ''
    if (service.runtime.binary.revision.size() > 9) {
        revision = service.runtime.binary.revision[0..9]
    } else {
        revision = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def versionSet = "-version ${version}-${revision}"
    def baseSet = "-base ${baseDir}"
    def command = "addVersionFiles ${componentSet} ${versionSet} ${baseSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param name
 * @param date
 * @return
 **/
@NonCPS
def apiCreateVersion(service, deployContext, ucdToken, name, date) {
    println "********************************"
    println " UCD Create Version for ${name} "
    println "********************************"

    def version = service.runtime.binary.version
    def revision = ''
    if (service.runtime.binary.revision.size() > 9) {
        revision = service.runtime.binary.revision[0..9]
    } else {
        revision = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def nameSet = "-name ${version}-${revision}"
    def descSet = "-description ${version}-ear"
    def command = "createVersion ${componentSet} ${nameSet} ${descSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param baseDir
 * @param name
 * @param date
 * @return
 */
@NonCPS
def apiAddVersion(service, deployContext, ucdToken, baseDir, name, date) {
    println "*****************************"
    println " UCD Add Version for ${name} "
    println "*****************************"

    def version = service.runtime.binary.version
    def revision = ''
    if (service.runtime.binary.revision.size() > 9) {
        revision = service.runtime.binary.revision[0..9]
    } else {
        revision = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def versionSet = "-version ${version}-${revision}"
    def baseSet = "-base ${baseDir}"
    def command = "addVersionFiles ${componentSet} ${versionSet} ${baseSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param name
 * @param date
 * @return
 */
@NonCPS
def ucdSetVersionProperty(service, deployContext, ucdToken, name, date) {
    println "**************************************"
    println " UCD Set Version Property for ${name} "
    println "**************************************"

    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def revisionTrunc = ''
    if (service.runtime.binary.revision.size() > 9) {
        revisionTrunc = service.runtime.binary.revision[0..9]
    } else {
        revisionTrunc = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def versionPath = "${version}-${revision}"
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def versionSet = "-version ${version}-${revisionTrunc}"
    def valueSet = "-value ${versionPath}"
    def command = "setVersionProperty ${componentSet} ${versionSet} -name versionPath ${valueSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}


@NonCPS
def ucdSetVersionTarProperty(service, deployContext, ucdToken, name, date) {
    println "**************************************"
    println " UCD Set Version Property for ${name} "
    println "**************************************"

    def version = service.runtime.binary.version
    def revision = service.runtime.binary.revision
    def artifactName = service.runtime.binary.artifactName
    def revisionTrunc = ''
    if (service.runtime.binary.revision.size() > 9) {
        revisionTrunc = service.runtime.binary.revision[0..9]
    } else {
        revisionTrunc = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def versionPath = "${version}-${revision}"
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def versionSet = "-version ${version}-${revisionTrunc}"
    def valueSet = "-value ${artifactName}"
    def command = "setVersionProperty ${componentSet} ${versionSet} -name application.tar.filename ${valueSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}


def install(deployContext) {
    def ucdUrl = deployContext.deployment.ucd_url
    def wgetCmd = 'wget --no-check-certificate --quiet'
    sh """${wgetCmd} ${ucdUrl}/tools/udclient.zip ; \\
                                  unzip -o udclient.zip """
}

def install_by_url(ucdUrl) {
    def wgetCmd = 'wget --no-check-certificate --quiet'
    sh """${wgetCmd} ${ucdUrl}/tools/udclient.zip ; \\
                                  unzip -o udclient.zip """
}
/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @param name
 * @param date
 * @return
 */
@NonCPS
def ucdAddVersionLink(service, deployContext, ucdToken, name, date) {

    println "**********************************"
    println " UCD Add Version Link for ${name} "
    println "**********************************"

    def version = service.runtime.binary.version
    def revision = ''
    if (service.runtime.binary.revision.size() > 9) {
        revision = service.runtime.binary.revision[0..9]
    } else {
        revision = service.runtime.binary.revision
    }
    def ucdUrl = deployContext.deployment.ucd_url
    def udClient = "./udclient/udclient"
    def componentSet = "-component '${name}'"
    def versionSet = "-version ${version}-${revision}"
    def linkName = "-linkName \'Jenkins Build upload\'"
    def linkSet = "-link \"\$env.BUILD_URL\""
    def command = "addVersionLink ${componentSet} ${versionSet} ${linkName} ${linkSet}"
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl} ${command}"

    def request = sh(returnStdout: true, script: ucdCmd).trim()
    return request
}

/**
 * @param service
 * @param deployContext
 * @param ucdToken
 * @return
 */
@NonCPS
def ucdGenJSON(service, deployContext, ucdToken) {
    println "********************"
    println " Running ucdGenJSON "
    println "********************"
    def jsonGenerator = UDClient.ucdGenJSON(deployContext, service, ucdToken, env)
    return jsonGenerator
}

/**
 * @param deployContext
 * @param ucdToken
 * @param jsonFile
 * @return
 */
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

/**
 * @param deployContext
 * @param ucdToken
 * @param requestId
 * @return
 */
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

/**
 * @param deployContext
 * @param ucdToken
 * @param requestId
 * @return
 */
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

@NonCPS
def ucdSnapshot(deployContext, ucdToken, jsonFile) {
    println "**********************"
    println " Running UCD Snapshot "
    println "**********************"
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def ucdScript = "${ucdCmd} createSnapshot ${jsonFile}"
    def request = sh(returnStdout:true, script: ucdScript).trim()
    return request
}

@NonCPS
def ucdSnapshotEnvironment(deployContext, ucdToken, userInput) {
    println "**********************"
    println " Running UCD Snapshot "
    println "**********************"
    snapshot = userInput.snapshot_name
    application = userInput.app_name
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def setEnv = "-environment '${deployContext.env}'"
    def setApp = "-application '${userInput.app_name}'"
    def setName = "-name '${userInput.snapshot_name}'"
    def ucdScript = "${ucdCmd} createSnapshotOfEnvironment ${setEnv} ${setApp} ${setName}"
    def request = sh(returnStdout:true, script: ucdScript).trim()
    return request
}

@NonCPS
def ucdLockSnapshotConfig(deployContext, ucdToken, userInput) {
    println "**********************"
    println " Running UCD Snapshot "
    println "**********************"
    snapshot = userInput.snapshot_name
    application = userInput.app_name
    def udClient = "./udclient/udclient"
    def ucdUrl = deployContext.deployment.ucd_url
    def ucdCmd = "${udClient} -authtoken ${ucdToken} -weburl ${ucdUrl}"
    def ucdScript = "${ucdCmd} lockSnapshotConfiguration -snapshot '${snapshot}' -application '${application}'"
    def request = sh(returnStdout:true, script: ucdScript).trim()
    return request
}

/**
 * @param getVersion
 * @param service
 * @param name
 * @return
 */
boolean getVersionsJson(getVersion, service, name) {
    println "**************************"
    println " Parsing UCD Version Info "
    println "**************************"
    def cfgVersion = service.runtime.binary.version
    def cfgRevision = ''
    if (service.runtime.binary.revision.size() > 9) {
        cfgRevision = service.runtime.binary.revision[0..9]
    } else {
        cfgRevision = service.runtime.binary.revision
    }
    def versionName = "${cfgVersion}-${cfgRevision}"

    def versionData = "{ \"versions\": " + getVersion + "}"
    def versionParser = new UCDVersionParser(versionData)

    for (Object versionObject : versionParser.versions) {
        UCDVersions ucdVersion = versionObject
        println "Version Name :: " + ucdVersion.name
        println "Version ID :: " + ucdVersion.id
        if (ucdVersion.name == versionName) {
            println "Component: ${name} :: Version: ${versionName} :: Already Uploaded :: Skipping"
            return true
        }
    }
    return false
}

/**
 *
 * @param getVersion
 * @param service
 * @param name
 * @return
 */
def getLatestVersionUploadJson(getVersion, service, name) {
    println "**************************"
    println " Parsing UCD Version Info "
    println "**************************"
    def cfgVersion = service.runtime.binary.version
    def cfgRevision = ''
    if (service.runtime.binary.revision.size() > 9) {
        cfgRevision = service.runtime.binary.revision[0..9]
    } else {
        cfgRevision = service.runtime.binary.revision
    }
    def versionName = "${cfgVersion}-${cfgRevision}"

    def versionData = "{ \"versions\": " + getVersion + "}"
    def versionParser = new UCDVersionParser(versionData)

    for (Object versionObject : versionParser.versions) {
        UCDVersions ucdVersion = versionObject
        println "Checking Version Name: " + ucdVersion.name + " is Active and Not Archived"
        if ((ucdVersion.active == true) && (ucdVersion.archived == false)) {
            return ucdVersion.name
        }
    }
}

String getNexusArtifactNameFromRegex(artifactRegex, nexusURL) {
    def cmd = [
            'bash',
            '-c',
            '''unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY; for file in $(curl --insecure -s ''' + nexusURL + '''/ | grep 'href=\"''' + nexusURL + '''/' | sed 's/.*href="//'| sed 's/".*//' |  grep -oE ''' + artifactRegex + ''')
    |do
    |    echo $file
    |done'''.stripMargin()]

    def resultList = cmd.execute().text.readLines()
    def updatedResults = []
    for (Object result : resultList) {
        updatedResults.add(result.split('/').last())
    }
    " \n" + updatedResults.unique().join('\n')
    return resultList
}

