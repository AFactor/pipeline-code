# Readme for veracode pipelines

Vercode pipelines are designed to either download reports or upload artifacts to external veracode server.
Overall this is done to perform various security tests.

These separate pipelines are :

veracodeDownload:
Parameters:
  -appName          ->name of application that we want to test
  -pathToConfig     ->path to the config of the job in .json

veracodeUpload:
  -appName          ->name of application that we want to test
  -pathToConfig     ->path to the config of the job in .json
  -dirPathToUpload  ->path to the dir with artifacts to upload
  -pathToBuildScript->path to the script that builds the stash (see Building the stash secrion below)
NOTE: function that builds the stash must be called runTest and take targetBranch and context as a parameters (name is due to backwards compatibility)
    runTest(targetBranch, context)

Inside of the json config for the job  we expect to have :
1. veracode.id -> fail if not set
2. veracode.credentials -> use 'veracode-creds' if not set
3. config.veracode.artifacts -> use 'artifacts' for stash name if not set
4. veracode.notificationList -> use 'LloydsCJTDevOps@sapient.com' if not set
5. config.splunk.reportdir -> fail if not set


example invocations (for pas):
veracodeDownload('pas-bigdata-common', 'pipelines/conf/job-configuration.json')
veracodeUpload('pas-bigdata-common', 'pipelines/conf/job-configuration.json', 'common-audit/target', 'pipelines/tests/veracode.groovy')

Building the stash:
There are several options when building the stash with artifacts to upload by verracode pipeline:

1. Passing name of build script
Function that builds the stash must be called runTest and take targetBranch and context as a parameters (name is due to backwards compatibility) runTest(targetBranch, context)
You can also create a wrapper script that conforms to this interface and inside do any custom calls. Just note that resulting stash still has to be called 'artifacts'. Example of such script:

def runTest(String targetBranch, context) {
  // Let unit tests build 'artifacts' stash
  node(){
    unit = load('pipelines/tests/unit.groovy')
    unit.runTest(targetBranch, context)
  }
}

2. Configuring stash name
Upload pipeline has logic to skip build step in case stash name is configured in job config. In that case, it will try to unstash this and upload artifact from there. It is then up to you to prepare artifact in the configured stash before invoking the pipeline. You can do that likely by running the same build step as in the main pipeline. Invoke that like in the wrapper script example above.
