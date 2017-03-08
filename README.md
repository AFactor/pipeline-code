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

### nexusPublisher
	Provides custom step to publish nexus artifacts as per Lloyds practice
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
		
#####		SCP(String source, String destination) 

#####		RSYNC(String source, String destination) 

### Utils  
	utility methods for usage within the sandbox

##### snapshotStatus(String imagefile)
	Pretty Snapshot for the current bluild status rendered to imagefile

### ServiceDiscovery
	Beta implementation of Service Discovery. 
	Currently only provides  `(new ServiceDiscovery).locate('vault')`
	
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

