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

The Jenkinsfile currently is very similar to Eagle, however we also deal with parameterized builds here.

UCD Pipeline supports 2 types of parameterized builds:

Method 1: Through job codebase,  

Method 2: Through devops-jenkins2-jobspec ucd_params.groovy calls

We are currently using method 1, an example Jenkinsfile used in job codebases is as follows:


Example:

```groovy

@Library(['workflowlib-ucd-global', 'workflowlib-sandbox'])
import com.lbg.workflow.ucd.*
import com.lbg.workflow.sandbox.*
import com.lbg.workflow.sandbox.deploy.UtilsUCD

def utils = new UtilsUCD()
String artifactNames = utils.getNexusArtifactNameFromMetadata('sales-pca-api-ear-v1','http://nexus.sandbox.extranet.group/nexus/content/repositories/releases/com/lbg/ib/api/sales/sales-pca-api-ear-v1')

String choices = 'yes\nno'
String defaultNoChoices = 'no\nyes'
String deployChoices = 'Deploy application/cluster and restart WAS'

properties([
        buildDiscarder(logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '5', daysToKeepStr: '30', numToKeepStr: '5')),
        [$class: 'RebuildSettings', autoRebuild: true, rebuildDisabled: false],
        parameters([
        string(defaultValue: '',description: 'Please select the name of the artifact you wish to deploy', name: 'artifactName'),
        choice(choices: choices, description: 'Do you wish to deploy?', name: 'deploy'),
        choice(choices: ucdWASHandler('DigitalMC_Sales WAS Cluster'), description: 'Please Provide the WAS Version for Deployment', name: 'wasVersion'),
        choice(choices: choices, description: 'Upload the selected artifact from nexus to UrbanCode', name: 'upload'),
        choice(choices: defaultNoChoices, description: 'Execute PreBDD phase?', name: 'pre_bdd'),
        choice(choices: defaultNoChoices, description: 'Execute PostBDD phase?', name: 'post_bdd'),
        choice(choices: deployChoices, description: 'Which deployment process do you wish to execute?', name: 'process'),
        choice(choices: defaultNoChoices, description: 'Deploy changes only? (If unsure leave as-is)', name: 'onlyChanged')
        ])
])

def configuration

node {
    checkout scm
    def jobName = "${env.JOB_NAME}"
    def targetEnv = jobName.split('/').last()
    configuration = "pipelines/conf/${targetEnv}/job-configuration.json"
    echo "Job: ${jobName} :: ENV: ${targetEnv} :: Configuration: ${configuration}"
    if (!configuration) {
        error "Invalid job configuration :: ${configuration}"
    }
}

invokeDeployPipelinePhoenix(configuration)

```

If Parameterized build options are not required the following structure can be used:


```groovy

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
  "journey": "ucd-mca",
  "env": "ST04 (ST04b)",
  "label": "lbg_slave",
  "metadata": {
    "confluence": {
      "type": "ucd-mca",
      "server": "https://confluence.devops.lloydsbanking.com",
      "page": "26784379",
      "credentials": "confluence-publisher"
    },
    "notifyList": "lloydscjtdevops@sapient.com"
  },
  "services": [
    {
      "components": [
        {
          "name": "DigitalMC_sales-api-savings Application",
          "baseDir": "dist/urbancode"
        }
      ],
      "type": "api",
      "deploy": true,
      "upload": true,
      "name": "Digital - MCA Sales",
      "description": "MCA Sales",
      "runtime": {
        "binary": {
          "nexus_url": "http://nexus.sandbox.extranet.group/nexus/content/repositories/releases/com/lbg/ib/api/pao/sales-api-savings-ear",
          "version": "",
          "name": "",
          "extension": "",
          "revision": ""
        }
      },
      "env": {
        "NODE_ENV": "ST04 (ST04b)"
      }
    }
  ],
  "deployment": {
    "type": "ucd",
    "ucd_url": "https://ucd.intranet.group",
    "work_dir": "dist/urbancode",
    "process": "Stage and Activate",
    "credentials": "UC_TOKEN_MCA",
    "timeout": 120
  },
  "proxy": {
    "deploy": false
  },
  "tests": {
    "type": "api",
    "repo": "pao-savings-api",
    "pre_bdd": true,
    "post_bdd": true,
    "branch": "",
    "credentials": "gerrit-admin"
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
There is a service.type that defines the type of service and this will be up to each individual team to correctly identify and setup.
Most types will actually fall into the predefined api / cwa or ob-aisp, if what is being deployed is slightly please add this new service type in. 
The Service flows as follows:

```groovy
vars/phoenixDeployStage
    _|----> vars/phoenixDeployService.groovy
```


The actual service fields are defined in:
```
src.com.lbg.workflow.sandbox.deploy.phoenix.deployContext.groovy 
            |
           _|----->src.com.lbg.workflow.sandbox.deploy.phoenix.Service.groovy
```

As service is an array of hashes, the Service.groovy defines the further types the service can offer, it is run through the constructor
of deployContext to ensure it is not lazymapped. Lazy maps are not serialiazable and  all jenkins jobs must be serialiazable. 

bluemix information is defined here as well as ucd information. if you would like different keys to be defined for a service
you wish to deploy it will need to be added here, there is also the metadata hash which can take any key/value pair if you dont
wish to explicitly define anything. Generally speaking these shouldnt need to be modified and should cover everything necessary for ucd deployments,
if in the future another type of deployment is to be added, then the extra fields required for the deployment, can be defined in the above files. 

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

Only the nexus_url needs to be specified as the artifactName is passed as a parameter and the rest of the fields are then filled out from the artifact. 

Within vars/phoenixDeployService.groovy there are methods defined for api and cwa:


```groovy
private void apiArtifactPath(service) {
    srvBin = service.runtime.binary
    def artifactName = ''
    if (!srvBin.artifact) {
        def nameComp = srvBin.artifactName.split(/\./)
        println ("Name Components == " + nameComp)
        if (nameComp.size() >= 2) {
            if ('tar' == nameComp[-2]) {
                srvBin.extension = srvBin.artifactName.split(nameComp[0]).last()
                artifactName = srvBin.artifactName.split("[.]" + srvBin.extension)[0]
            } else {
                srvBin.extension = nameComp.last()
                artifactName = srvBin.artifactName.split("[.]" + srvBin.extension)[0]
            }
        }
        def lastDash = artifactName.split('-').last()
        if (lastDash =~ /^\w{7}$/) {
            srvBin.version = artifactName.split('-')[-2]
        } else {
            srvBin.version = lastDash
        }
        def verDash = '-' + srvBin.version
        srvBin.name = artifactName.split(verDash)[0]
        srvBin.artifact = srvBin.nexus_url + "/" + srvBin.version + "/" + srvBin.artifactName
        echo "Artifact Name: ${artifactName} :: Last Dash: ${lastDash} :: Version: ${srvBin.version} :: artifact: ${srvBin.artifact}"
        createScript = "touch version_${srvBin.version}"
        sh(returnStdout:true, script: createScript)
        archiveArtifacts "version_${srvBin.version}"
    }
}
```

This deconstructs the artifact name which is passed in as a parameter and then uses the various identifiers in the name to fill out all of the information required
This then works the same way as the bluemix method, where artifact contains the full nexus path and artifactName contains just the name, this is further augmented with 
the apiExtract method - which gathers anything missing directly from the artifact and prepares the artifact for upload to UCD.


##### Service Componenets

For ucd there is a further :

```
src.com.lbg.workflow.sandbox.phoenix.Components.groovy
```

which is again an array of hashes and determines what components are part of the upload / deployment and this is actually where
it should be deployed to within the environment defined within the json file. The basedir and name must exist in UCD for the deployment
to succeed.


##### Tests

The Tests section is for pre and post-bdd testing. The pre-bdd side has been deprecated for API and CWA atleast, as it was deemed unneccessary to test pre-existing deployments, 
but for post-bdd this is used. The fields are as follows:

```json
"tests": {
    "type": "api",
    "repo": "pao-savings-api",
    "pre_bdd": true,
    "post_bdd": true,
    "branch": "",
    "credentials": "gerrit-admin"
  }
```

the type again determines what sort of testing can be carried out and the tests are defined in /vars/phoenixTestStage.groovy. 

The repo is important, this is the code repo that the artifact was built from and the repo which should contain the code tests, the other important field is
branch and this defines what branch the artifact was built from. The pre-bdd and post-bdd options are both parameterized build options which are presented at build time. 
The credentials shouldnt need to be changed. 

If you would like to add custom steps please modify phoenixTestStage.grooovy. 


##### Deploy / Upload Service

Finally within service there is a deploy and upload boolean flags. Deploy works for both bluemix and ucd, upload is ucd only. 
If deployment is false and upload is true the artifact will only be uploaded to ucd, vice-versa and the artifact will only be deployed. 

