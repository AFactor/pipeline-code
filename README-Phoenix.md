# Phoenix Pipeline

## Introduction

The phoenix pipeline is an extension of the eagle pipeline developed by Ram Guda, 
the main difference is that it is designed to handle ucd and bluemix deployments. 


## Structure
The structure, is as follows:

```
vars/invokeDeployPipelinePhoenix
|
|-> Deployment Type: UCD
     |-> phoenixTestStage.groovy (all tests defined here)
     |    
     |-> phoenixDeployStage.groovy 
     |     |-> phoenixDeployService.groovy
     |    _|_  |-> Deploy Type: UCD
     |         |    |-> Service Type (cwa / api etc):
     |         |   _|_       Archive Extraction 
     |         |             apiExtract / cwaExtract / bluemixExtract etc..
     |         |                  |--> Type: Upload
     |         |                  |      |-> phoenixUploadUCDService.groovy
     |         |                  |           |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
     |         |                  |           |___/---> Do the actual deployment
     |         |                  |                     This also calls devops-pipeline-ucdlibs-global
     |         |                  |--> Type: Deploy
     |         |                  |     |-> phoenixDeployUCDService.groovy
     |         |                  |           |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
     |         |                  |           |___/---> Do the actual deployment
     |         |                 _|_                    This also calls devops-pipeline-ucdlibs-global
     |         |           
     |         |   
     |         |-> Deploy Type: Bluemix
     |             Archive Extraction: bluemixExtract
     |               |
     |               |-> eagleDeployBluemixService.groovy (Eagle Pipeline Takes over)
     |              _|_
     |
     |
     | -> phoenixNotifyStage.groovy
          This handles confluence, splunk, email ... Notifications 
```

On top of this there is a phoenixLogger.groovy, this can be used to generate different types of messages
and does support colour output (although currently disabled)


## Usage:

### Jenkinsfile

The Jenkinsfile usage is the exact same as Eagle, with the exception that workflowlib-ucd-global is also invoked.

Example:

```
@Library(['workflowlib-ucd-global', 'workflowlib-sandbox@master'])
import com.lbg.workflow.ucd.*
import com.lbg.workflow.sandbox.*

properties([
        buildDiscarder(logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '5', daysToKeepStr: '30', numToKeepStr: '5')),
        [$class: 'RebuildSettings', autoRebuild: true, rebuildDisabled: false]
])

def configuration

node {
    checkout scm
    def jobName = "${env.JOB_NAME}"
    echo "JOB NAME == ${jobName}"
    def targetEnv = jobName.split('/').last()
    configuration = "pipelines/conf/${targetEnv}/job-configuration.json"
    echo "Target ENV Currently: ${targetEnv} :: Configuration:  ${configuration}"
    if (!configuration) {
        error "Invalid job configuration :: ${configuration}"
    }
}

invokeDeployPipelinePhoenix(configuration)

```    

### Json Configuration Data
This is the biggest departure from the Eagle pipeline, the json file has been reconfigured to handle different types of deployments.

The json File is as follows:

```
{
  "journey": "cwa-ucd",
  "env": "DEV",
  "label": "lbg_slave",
  "metadata": {
    "confluence": {
      "type": "cwa-ucd",
      "server": "http://confluence.sandbox.extranet.group",
      "page": "26784379",
      "credentials": "confluence-publisher"
    },
    "notifyList": "vhedayati@sapient.com"
  },
  "services": [
    {
      "components": [
        {
          "baseDir": "dist/bos/content/46-7cd599c733aa46c1687a45ebe04b6e60b2923eea",
          "name": "IB-PB-CWA-B-PCA-AVA"
        },
        {
          "baseDir": "dist/halifax/content/46-7cd599c733aa46c1687a45ebe04b6e60b2923eea",
          "name": "IB-PB-CWA-H-PCA-AVA"
        },
        {
          "baseDir": "dist/lloyds/content/46-7cd599c733aa46c1687a45ebe04b6e60b2923eea",
          "name": "IB-PB-CWA-L-PCA-AVA"
        }
      ],
      "type": "cwa",
      "deploy": true,
      "upload": true,
      "name": "Digital - CWA - Personal Current Account - AVA",
      "description": "Personal Current Account Opening for AVA versions to stage and Activate on DEV",
      "runtime": {
        "binary": {
          "nexus_url": "http://nexus.sandbox.extranet.group/nexus/content/repositories/releases/pca-ava-artifacts",
          "version": "46",
          "name": "pca-cwa-packaged-accounts-master-artifact",
          "extension": "tar.gz",
          "revision": "7cd599c733aa46c1687a45ebe04b6e60b2923eea"
        }
      },
      "env": {
        "NODE_ENV": "DEV"
      }
    }
  ],
  "deployment": {
    "type": "ucd",
    "ucd_url": "https://ucd.intranet.group",
    "work_dir": "dist",
    "process": "Stage and Activate",
    "credentials": "UC_TOKEN_CWA"
  },
  "proxy": {
    "deploy": false
  }
}
```

#### Deployment
There is a type, this currently supports bluemix or ucd, the key / values are defined in:

src.com.lbg.workflow.sandbox.deploy.phoenix.deployContext.groovy deployment itself is a hashmap, so you can add any key: value pair
and as long as the deployment understands the keys then the deployment will be carried out. 

#### Service
This is a bit more complicated as there can be multiple different services, 

This is defined in:
src.com.lbg.workflow.sandbox.deploy.phoenix.deployContext.groovy 

which is further defined in:
src.com.lbg.workflow.sandbox.deploy.phoenix.Service.groovy

As service is an array of hashes, the Service.groovy defines the further types the service can offer, it is run through the constructor
of deployContext to ensure it is not lazymapped. Lazy maps are not serialiazable and  all jenkins jobs must be serialiazable. 

bluemix information is defined here as well as ucd information. if you would like different keys to be defined for a service
you wish to deploy it will need to be added here, there is also the metadata hash which can take any key/value pair if you dont
wish to explicitly define anything. 

##### Artifact Information
Within service the following extras are defined:
src.com.lbg.workflow.sandbox.phoenix.deploy.ServiceRuntime.groovy 
and 
src.com.lbg.workflow.sandbox.phoenix.deploy.ServiceRuntimeBinary.groovy

For bluemix only artifact is used and the whole path to the artifact is defined in artifact, for ucd 
the artifact is split into multiple sections, which are as follows:

```
    String nexus_url
    String version
    String revision
    String extension
    String name
    String artifactName
    String artifact
```

Within vars/phoenixDeployService.groovy there are methods defined for api and cwa:

```
private void apiArtifactPath(service) {
    srvBin = service.runtime.binary
    if (!srvBin.artifact) {
        srvBin.artifactName = srvBin.name + "-" + srvBin.version + "." + srvBin.extension
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.version + "/" + srvBin.artifactName
    }
```
This constructs the artifact from the component parts and fills artifactName and artifact and this then works the same
way as the bluemix method where artifact contains the full nexus path and artifactName contains just the name, this allows
any specific naming scheme or nexus path to work and doesnt rely on what can be a problematic split to try and work out
how the naming scheme for the artifact and the nexus upload path is defined. 

##### Service Componenets
For ucd there is a further :
src.com.lbg.workflow.sandbox.phoenix.Components.groovy

which is again an array of hashes and determines what components are part of the upload / deployment and this is actually where
it should be deployed to within the environment defined within the json file. The basedir and name must exist in UCD for the deployment
to succeed.

##### Deploy / Upload Service
Finally within service there is a deploy and upload boolean flags. Deploy works for both bluemix and ucd, upload is ucd only. 
If deployment is false and upload is true the artifact will only be uploaded to ucd, vice-versa and the artifact will only be deployed. 

