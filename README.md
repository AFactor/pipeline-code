# Jenkins/Pipeline workflow-cps-global-lib libraries

### designed to run within the groovy sandbox

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

## Provides: 

### invokeBuildPipelineHawk
	Pipeline Template: Hawk
	Unit ->
	Static Analysis (checkstyle/sonar) ->
	Build ->
	Deploy ->
	Integration Tests (BDD/accessibility/performance) ->
	Cleanup ->
	Publish (nexus/splunk)
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
			nexusURL = "Target URL"
			artifact = "Artifact File Name"
			credentialsID = "Jenkins Credentials to use"
	}
```


### sonarTest 
###(DEPRECATED)
	Provides custom step to run a sonar test against a Jenkins defined Server
```
	sonarTest {
			sonarServer = 
			sonarProject = 
			targetBranch = 
			qualityGate = 
			exclusions = 
			coverageExclusions =
			coverageReportFile = 
			coverageStash = 
		}
```

### sonarRunner 
#### (Preferred method to run SonarQube Scanner )
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
		SCP(String source, String destination) 

		RSYNC(String source, String destination) 
