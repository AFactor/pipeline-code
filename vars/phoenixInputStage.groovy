import com.lbg.workflow.sandbox.deploy.UtilsUCD
import com.lbg.workflow.ucd.HTTPConnector

private def call(deployContext) {
    // Gathering Artifact Names
    String yesChoices = 'yes\nno'
    String noChoices = 'no\nyes'
    String deployProc = deployContext.deployment.process.join('\n')
    def choiceList = []
    def wasVersions = ''
    def setChoice = yesChoices
    def artifactNames
    def snapshotVersions
    node(deployContext.label) {
        withCredentials([string(credentialsId: deployContext.deployment.credentials, variable: 'ucdToken')]) {
            withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
                for (def service in deployContext.services) {
                    if (service.type == 'api') {
                        setChoice = noChoices
                    }
                }
                artifactNames = artifactGather(deployContext)
                for (artifactMap in artifactNames) {
                    def srvName  = artifactMap.key
                    def artifactList = artifactMap.value
                    //def artifactList = artifactMap.value.join('\n')
                    choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                                   'description': 'Select Artifact:',
                                   'name'       : "artifacts-${srvName}",
                                   'choices'    : artifactList ]

                }
                snapshotVersions = snapshotVersionGather(deployContext, ucdToken)
                for (snapAppMap in snapshotVersions) {
                    def snapshots = snapAppMap.value
                    choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                                   'description': 'Select Artifact:',
                                   'name'       : "snapshots-${snapAppMap.key}",
                                   'choices'    : snapshots]
                }
                if (deployContext.journey == 'ucd-mca') {
                    wasVersions = componentVersionGather(ucdToken, 'DigitalMC_Sales WAS Cluster')
                } else if (deployContext.journey == 'ucd-api') {
                    wasVersions = componentVersionGather(ucdToken, 'DigitalIB_Sales WAS Cluster')
                } else if (deployContext.journey == 'ucd-salsa') {
                    // not sure what the WAS Component for Salsa is
                    wasVersions = 'unknown'
                }

                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                               'description': 'Do you wish to upload?',
                               'name'       : 'upload',
                               'choices'    : yesChoices]
                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                               'description': 'Do you wish to deploy?',
                               'name'       : 'deploy',
                               'choices'    : yesChoices]
                if (!wasVersions.isEmpty()) {
                    choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                                   'description': 'Select Appropriate WAS Version',
                                   'name'       : 'wasVersion',
                                   'choices'    : wasVersions]
                }
                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                               'description': 'Execute PostBDD phase?',
                               'name'       : 'post_bdd',
                               'choices'    : noChoices]
                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                               'description': 'Select Deploy Process:',
                               'name'       : 'process',
                               'choices'    : deployProc]
                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                               'description': 'Deploy changes only? (If unsure leave as-is)',
                               'name'       : 'onlyChanged',
                               'choices'    : setChoice]
                choiceList += ['$class'     : 'hudson.model.ChoiceParameterDefinition',
                                'description': 'Perform Hot Deploy? (If unsure leave as-is)',
                                'name'       : 'hotDeploy',
                                'choices'    : noChoices]
            }
        }
    }
    echo "Provided Choices for User Input :: ${choiceList}"
    return choiceList
}

private def artifactGather(deployContext) {
    UtilsUCD utils = new UtilsUCD()
    def artifactNames = [:]
    for (def service in deployContext.services) {
        def srvName = service.name
        def srvBin = service.runtime.binary
        switch(service.type) {
            case 'cwa':
                def nexusUrl = srvBin.nexus_url
                def regex = srvBin.regex
                def artifactList = []
                artifactList += ''
                HTTPConnector connect = new HTTPConnector(deployContext, nexusUrl, regex)
                artifactList += connect.GetNexusArtifactFromHttp()
                artifactNames[srvName] = artifactList.join('\n')
                break
            case 'api':
                def nexusUrl = srvBin.nexus_url
                HTTPConnector connect = new HTTPConnector(deployContext, "${nexusUrl}/", 'maven-metadata.xml')
                def data = connect.GetNexusArtifacts()
                def artifactBaseName = data.artifactId
                def artifactList = []
                artifactList += ''
                for (def version in data.versioning.versions.version) {
                    artifactList += artifactBaseName.toString() + '-' + version.toString() + '.ear'
                }
                artifactNames[srvName] = artifactList.join('\n')
                break
        }
    }
    return artifactNames
}

private def componentVersionGather(ucdToken, String name) {
    def utils = new UtilsUCD()
    def versionsChoice = utils.ucdComponentVersionGather(ucdToken, name)
    return versionsChoice
}

private def snapshotVersionGather(deployContext, ucdToken) {
    def snapPerApp = [:]
    for (def service in deployContext.services) {
        def srvName = service.name
        def utils = new UtilsUCD()
        def snapshotsChoice = utils.ucdSnapshotVersionGather(ucdToken, srvName)
        snapPerApp[srvName] = snapshotsChoice
    }
    return snapPerApp
}

def mergeData(deployContext, userInput) {
    deployContext.deployment.process = userInput.process

    deployContext.deployment.properties = [:]
    // We can pass any number of properties through the json here.
    // All we need to define is what property the input maps to.
    if(userInput.hotDeploy == "yes") {
        deployContext.deployment.properties['hot.deploy.requested'] = "true"
    }

    deployContext.tests.post_bdd = convertYesNoToBoolean(userInput.post_bdd)
    echo "Selected User Input :: ${userInput}"
    for (def service in deployContext.services) {
        service.deploy = convertYesNoToBoolean(userInput.deploy)
        service.upload = convertYesNoToBoolean(userInput.upload)
        service.onlyChanged = convertYesNoToBoolean(userInput.onlyChanged)
        def srvName = service.name
        for (userInputMap in userInput) {
            if (userInputMap.key.contains('snapshots-')) {
                def userSrvName = userInputMap.key.split('snapshots-').last()
                echo "Matched Snapshot :: Key: ${userSrvName}"
                if (userSrvName == srvName) {
                    service.snapshot = userInputMap.value
                }
            } else if (userInputMap.key.contains('artifacts-')) {
                def userSrvName = userInputMap.key.split('artifacts-').last()
                echo "Matched Artifact :: Key: ${userSrvName}"
                if (userSrvName == srvName) {
                    service.runtime.binary.artifactName = userInputMap.value
                }
            }
        }
    }
}

private boolean convertYesNoToBoolean(value){
    return value == 'yes'
}

return this;

