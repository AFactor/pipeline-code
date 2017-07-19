# Jenkins/Pipeline workflow-cps-global-lib libraries

##### designed to run within the groovy sandbox

### invokeBuildPipelineHawk 

#### Full Workflow

	Pipeline Template: Hawk
	Unit ->
	Static Analysis (checkstyle/sonar/...) ->
	Build ->
	Deploy ->
	Integration Tests (BDD/accessibility/performance/...) ->
	Cleanup ->
	Publish (nexus/splunk)
	
#### Integration Workflow
	branches : `develop, master, hotfixes(.*), release-prod(.*)`
	Full workflow

#### Feature Workflow
	branches : `sprint[0-9]+/.+`
	Full Workflow sans Splunk/Nexus Publications

#### Patchset Workflow
	branches : auto-detected
	Full Workflow sans BDD and Splunk/Nexus Publications


## Usage:

#### Sample 1 
```
@Library('workflowlib-sandbox@v1.0.0')
import com.lbg.workflow.sandbox.*

def configuration = "pipelines/conf/job-configuration.json"
BuildHandlers handlers = (BuildHandlers) new CWABuildHandlers()

invokeBuildPipelineHawk( 'pca-sales-cwa', handlers, configuration )

```

#### Sample 2 
```
@Library('workflowlib-sandbox@v1.0.1')
import com.lbg.workflow.sandbox.*

def builder = 'pipelines/build/package.groovy'
def deployer = 'pipelines/deploy/application.groovy'
def unitTests = 	  [	'pipelines/tests/unit.groovy']

def staticAnalyses =  [	'pipelines/tests/sonar.groovy', 
						'pipelines/tests/checkstyle.groovy' ]
						
def integratonTests = [	'pipelines/tests/performance.groovy',
						'pipelines/tests/accessibility.groovy',
						'pipelines/tests/bdd.groovy']					

def configuration = "pipelines/conf/job-configuration.json"
BuildHandlers handlers = new ConfigurableBuildHandlers(	builder, 
														deployer, 
														unitTests, 
														staticAnalyses, 
														integrationTests) as BuildHandlers
	
invokeBuildPipelineHawk( 'your-api-codebase', handlers, configuration )
```

## Also Provides: 


### appName
	Utility method to construct an appname which is used consistently across tasks for synchronization

### gerritHandler
	Provides Utility methods to post gerrit updates, as well as query target branchnames


	buildStarted(String changeID, String revision)
	failCodeReview (String changeID, String revision )
	failTests (String changeID, String revision) 
	passCodeReview (String changeID, String revision ) 
	passTests (String changeID, String revision) 
	findTargetBranch(String targetCommit)

### gavNexusUploader
	
	Provides custom step to uload artifacts to a Nexus repository using (GAV) upload
```
	nexusPublisher {
			  nexusAPI = Upload API for nexus. Defaults to http(s)://NEXUSSERVER/service/local/artifact/maven/content'
              artifactPath = Full path to the uploadable file
              groupId = (G)  maven groupId
              artifactId = (A) maven artifactId
              version = (V) maven version of the artifact
              packaging = packaging/extension of the uploadable file. Tested for tar.gz,zip,war,jar

	}
```

### nexusPublisher
	Provides custom step to do a simple upload of an artifact to a Nexus url
	
```
	nexusPublisher {
			targetURL = "Target URL"
			tarball = "Artifact File Name"

	}
```


### sonarRunner 
	Provides custom step to run a soarQube runner/scanner test against a Jenkins defined Server. 
	Credentials are automatically injected.
```
	sonarTest {
			sonarServer = 'SonarServerID on Jenkins'
			preRun = '~/.bashrc'
			javaOptions = ['-Dsonar.sources: 'somefodler', '-Dappname': 'somename', ...]
		}
```

### splunkPublisher
	Provides custom step, that makes a callback to publishSplunk() method of supplied testScenarios. 
	Also provides utility methods that understand how to interact with the target splunk instance, thus abstracting those details from the end user.

### stormRunner

    Can be used to deploy a storm jar from jenkins node (master or slave) pushing to stormNode and
    invoking ssh to run 'storm kill <topology name>' then 'storm jar <jarfile> <classname>'
```
stormRunner {
    node = stormNode
    remoteUser = stormRemoteUser
    sshUser = stormSSHUSer
    stormTopClass = stormTopologyClass
    stormTopName = stormTopologyName
 
}
```
sample job-configuration.json

```
"storm": {
  "deploy_node": "shared-pas-01.sandbox.local",
  "remote_user": "pasuser",
  "ssh_user": "cslave",
  "topology_class": "com.lbg.pas.alerts.bigdata.audit.topology.TopologyMain",
  "topology_name": "CommsMgrAudit"
}
```

### stormKiller

Kills a storm topology

```
  stormKiller {
        node = stormNode
        remoteUser = stormRemoteUser
        sshUser = stormSSHUSer
        stormTopName = stormTopologyName
  }
```

		
####		SCP(String source, String destination) 

#####		RSYNC(String source, String destination) 

### Utils  
	utility methods for usage within the sandbox

##### snapshotStatus(String imagefile)
	Pretty Snapshot for the current bluild status rendered to imagefile

### ServiceDiscovery
	Beta implementation of Service Discovery. 
	Currently only provides  `(new ServiceDiscovery).locate('vault')`
	
### createDockerContext
	Loads a named "dockerContext". Translates to copying a well know set of Dockerfiles, docker-compose-yml, and .dockerignore
	for a known target environment
	Usage	
``` createDockerContext('node48') 
```

	Effect  creates Dockerfile.node.allmodules, Dockerfile.node.base, docker-compose.yml, and dockerignore for node:4.8, and service containers/environments
	node-allmodules, node-static, node-bdd-sauce, node-bdd-embedded-zap
	
	Further usable as 
```
	touch -c -t 12101630  * || true
	rm -rf node_modules
	rm -rf zap-report ; mkdir -p zap-report
	docker-compose build node-bdd-embedded-zap
	docker-compose run --rm node-bdd-embedded-zap bash -c "ln -sf ../node_modules; ${invokeBDD} "
	zapContainer=`docker-compose ps |grep zapx |  head -1 | awk {'print $1'}`
	sudo docker exec $zapContainer zap-cli report -f html -o report.html
	sudo docker exec $zapContainer zap-cli report -f xml -o report.xml
	sudo docker exec $zapContainer cat report.html >zap-report/report.html
	sudo docker exec $zapContainer cat report.xml >zap-report/report.xml
	docker-compose kill
	docker-compose rm -f
```	
	
### withGenericVaultSecrets
	Build wrapper for injecting secrets dynamically into your build from hashicorp Vault.
	Uses ServiceDiscovery to locate the vault service, recovers the secrets and injects into the build.
	Requires VAULT_TOKEN to be available in env. 
	Note: WHile this may work for other secret types, its only tested for the generic secret backend
```
        withGenericVaultSecrets ( [  'SAUCE_USER': 'CI/users/PAO/SAUCE_USER_DAY/user',
                                    'SAUCE_KEY': 'CI/users/PAO/SAUCE_USER_DAY/key']) {
                   /*
                   * Your BODY of work
                   */
        }

```
	You should typically invoke this within a credentials block
```
	withCredentials([string(credentialsId: 'pao-vault-token', variable: 'VAULT_TOKEN')]) {
		withGenericVaultSecrets ...
	}
```  


### Deployment pipeline 

#### invokeDeployPipelineEagle

	Pipeline Template: Eagle
	Initialise ->
        Build DeployContext from configuration
	Deploy Services ->
	    Deploy services - uses eagleDeployService per service
	Deploy Proxy ->
	    Deploy proxy - uses eagleDeployProxy
	Tester ->
	    Test services and proxy - eagleDeployTester
    Cleanup ->
	
#### eagleDeployService

    -> DeployService
        -> eagleDeployBluemixService (bluemix deployment)

#### eagleDeployProxy

    -> Deploy Proxy
        -> eagleDeployBluemixProxy
        
#### eagleDeployTester.groovy

    -> Test deployed services
    -> Test deployed proxy
    

### Usage:

```
@Library('workflowlib-sandbox@v2.8.1')
import com.lbg.workflow.sandbox.*

def configuration
node {
    checkout scm
    def jobName = "${env.JOB_NAME}"
    def targetEnv = jobName.substring(jobName.lastIndexOf('-') + 1)
    configuration = "pipelines/conf/${targetEnv}/job-configuration.json"
    if (configuration == null || !fileExists(configuration)) {
        error 'Invalid job configuration'
    }
}

invokeDeployPipelineEagle(configuration)

```    
