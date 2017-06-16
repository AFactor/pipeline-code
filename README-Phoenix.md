# Phoenix Pipeline

## Introduction

The phoenix pipeline is an extension of the eagle pipeline developed by Ram Guda, 
the main difference is that it is designed to handle ucd and bluemix deployments. 


## Structure
The structure, is as follows:

```
vars/invokeDeployPipelinePhoenix
|
|-> Stage:  Initialize -> Read in Json Configuration File
|
|-> Deployment Type: UCD
|     |
|     |-> Stage: Environment Check
|     |     |-> phoenixTestStage.groovy (all tests defined here)
|     |    _|_
|     |
|     |-> Stage: PRE-BDD Check
|     |     |-> phoenixTestStage.groovy
|     |    _|_    |-> bddTests
|     |          _|_   | 
|     |                |-> Service: api
|     |                |     |-> apiBddCheck
|     |                |    _|    |-> Load pipelines/tests/bdd.groovy
|     |                |         _|_       Which in turn checks out code repo
|     |                |                   and executes bdd tests
|     |                | 
|     |                |-> Service: cwa 
|     |                |     |-> cwaBddCheck
|     |                |    _|_   |-> Currently Does Nothing
|     |                |         _|_       
|     |                |                   
|     |                |-> Service: salsa 
|     |               _|_     |
|     |                       |-> salsaBddCheck
|     |                      _|_   |-> Currently Does Nothing
|     |                           _|_       
|     |
|     |-> Stage: Upload Services >-----------\
|     |                                      |
|     |                                      |
|     |-> Stage: Deploy Services >-----------| 
|     |                                      | 
|     |                                      | 
|     |                                      |
|     |   phoenixDeployStage.groovy  <-------/
|     |     |-> phoenixDeployService.groovy
|     |    _|_  |-> Deploy Type: UCD
|     |         |    |-> Service Type (cwa / api etc):
|     |         |   _|_       Archive Extraction 
|     |         |             apiExtract / cwaExtract / bluemixExtract etc..
|     |         |                  |--> Type: Upload
|     |         |                  |      |-> phoenixUploadUCDService.groovy
|     |         |                  |     _|_   |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
|     |         |                  |          _|_   Do the actual deployment
|     |         |                  |                This also calls devops-pipeline-ucdlibs-global
|     |         |                  |--> Type: Deploy
|     |         |                  |     |-> phoenixDeployUCDService.groovy
|     |         |                  |    _|_    |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
|     |         |                  |          _|_    Do the actual deployment
|     |         |                  |                 This also calls devops-pipeline-ucdlibs-global
|     |         |                  |           
|     |         |                  |           
|     |         |                 _|_          
|     |         |           
|     |         |   
|     |         |-> Deploy Type: Bluemix
|     |        _|_    Archive Extraction: bluemixExtract
|     |                 |
|     |                 |-> eagleDeployBluemixService.groovy (Eagle Pipeline Takes over)
|     |                _|_
|     |
|     |-> Stage: POST-BDD Check
|     |     |-> phoenixTestStage.groovy - exact same as pre-BDD Check above currently
|     |    _|_
|     |
|     |-> Stage: TEST
|     |     |-> phoenixTestStage.groovy (all tests defined here)
|     |    _|_
|     |
|     |-> Stage: NOTIFY
|    _|_    |-> phoenixNotifyStage.groovy
|          _|_    This handles confluence, splunk, email ... Notifications 
|    
|
|-> Deployment Type: Bluemix
|     |
|     |
|     |-> Stage: Deploy Services >-----------\ 
|     |                                      | 
|     |                                      | 
|     |                                      |
|     |   phoenixDeployStage.groovy  <-------/
|     |     |-> phoenixDeployService.groovy
|     |    _|_  |-> Deploy Type: UCD
|     |         |    |-> Service Type (cwa / api etc):
|     |         |   _|_       Archive Extraction 
|     |         |             apiExtract / cwaExtract / bluemixExtract etc..
|     |         |                  |--> Type: Upload
|     |         |                  |      |-> phoenixUploadUCDService.groovy
|     |         |                  |     _|_   |
|     |         |                  |           |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
|     |         |                  |          _|_   Do the actual deployment
|     |         |                  |                This also calls devops-pipeline-ucdlibs-global
|     |         |                  |                
|     |         |                  |--> Type: Deploy
|     |         |                  |     |-> phoenixDeployUCDService.groovy
|     |         |                  |    _|_    |
|     |         |                  |           |-> com.lbg.workflow.sandbox.deploy.UtilsUCD
|     |         |                  |          _|_    Do the actual deployment
|     |         |                 _|_                This also calls devops-pipeline-ucdlibs-global
|     |         |           
|     |         |   
|     |         |-> Deploy Type: Bluemix
|     |        _|_    Archive Extraction: bluemixExtract
|     |               |-> eagleDeployBluemixService.groovy (Eagle Pipeline Takes over)
|     |              _|_
|     |
|     |-> Stage: Deploy Proxy
|     |     |-> phoenixDeployService.groovy (deployProxy)
|     |    _|_
|     |
|     |-> Stage: TEST
|     |     |-> phoenixTestStage.groovy (all tests defined here)
|     |    _|_
|     |
|     |-> Stage: NOTIFY
|    _|_    |-> phoenixNotifyStage.groovy
|          _|_   This handles confluence, splunk, email ... Notifications 
x   
```

The basic flow is as follows, invokeDeployPipelinePhoenix is called, this sets out each stage of the pipeline. 
The pipeline itself is broken into 3 stage files, test, deploy and finally notify. 
The core of the code is within phoenixDeployService. 
For bluemix the Eagle pipeline takes over at this point, for UCD depending on whether or not it is an upload or deployment, 
phoenixUploadUCDService or phoenixDeployUCDService are called, both of these in turn call:

src.com.lbg.workflow.sandbox.deploy.UtilsUCD.groovy, 
This is a bunch of functions which each carry out a specific call to udclient 
with all of the information gathered from the json configuration file for the environment.

For deployment, UtilsUCD calls com.lbg.workflow.ucd.UDClient which is in devops-ucdlibs-global repo, 
this sits in an external lib as it creates a json file using groovy, something the sandbox env is not allowed to do. 
The json file is fed back and then pumped into the actual udclient (again all data from the json configuration file initially read in)
this in turn allows the deployment to be carried out. The json file UCD expects has a specific format and this is the job
of that external lib, which in essence means that the deployment should be identical regardless of service being deployed,
however the uploads will differ based on how the artifact is packaged and how it is expected to be uploaded to UCD. The upload methods
will probably need updating / additional code to work for each particular service attempting to be deployed. 

On top of this there is a phoenixLogger.groovy, this can be used to generate different types of messages
and supports colour output (although currently disabled)


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
  "journey": "ucd-cwa",
  "env": "DEV",
  "label": "lbg_slave",
  "metadata": {
    "confluence": {
      "type": "ucd-cwa",
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
    "credentials": "UC_TOKEN_CWA",
    "timeout": 60
  },
  "proxy": {
    "deploy": false
  }
}
```

#### Deployment
There is a type, this currently supports bluemix or ucd, the key / values are defined in:

```
src.com.lbg.workflow.sandbox.deploy.phoenix.deployContext.groovy 
```

deployment is a hashmap, so you can add any key: value pair and as long as the deployment
understands the keys then the deployment will be carried out. 

#### Service
This is a bit more complicated as there can be multiple different services, 

This is defined in:
```
src.com.lbg.workflow.sandbox.deploy.phoenix.deployContext.groovy 
```

which is further defined in:
```
src.com.lbg.workflow.sandbox.deploy.phoenix.Service.groovy
```

As service is an array of hashes, the Service.groovy defines the further types the service can offer, it is run through the constructor
of deployContext to ensure it is not lazymapped. Lazy maps are not serialiazable and  all jenkins jobs must be serialiazable. 

bluemix information is defined here as well as ucd information. if you would like different keys to be defined for a service
you wish to deploy it will need to be added here, there is also the metadata hash which can take any key/value pair if you dont
wish to explicitly define anything. 

##### Artifact Information
Within service the following extras are defined:

```
src.com.lbg.workflow.sandbox.phoenix.deploy.ServiceRuntime.groovy 
src.com.lbg.workflow.sandbox.phoenix.deploy.ServiceRuntimeBinary.groovy
```

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
```
src.com.lbg.workflow.sandbox.phoenix.Components.groovy
```

which is again an array of hashes and determines what components are part of the upload / deployment and this is actually where
it should be deployed to within the environment defined within the json file. The basedir and name must exist in UCD for the deployment
to succeed.

##### Deploy / Upload Service
Finally within service there is a deploy and upload boolean flags. Deploy works for both bluemix and ucd, upload is ucd only. 
If deployment is false and upload is true the artifact will only be uploaded to ucd, vice-versa and the artifact will only be deployed. 

