#!/usr/local/bin/groovy

import com.lbg.workflow.sandbox.*

def call(String appName, pathToConfig, pathToBuildScript)
{
  def deployer
  def context
  def tester
  def epoch
  def targetBranch
  def veracodeApiLock

  try {
    stage('Initialize'){
    	node(){
    		checkout scm
    		targetBranch = env.BRANCH_NAME
    		context = new BuildContext(appName, readFile(pathToConfig))
    		epoch = sh(returnStdout: true, script: 'date +%d%m%Y%H%M').trim()
                veracodeApiLock = context.config.veracode.id
    		deployer = new veracode()
    	}
    }
    milestone(label:'initialized')

    stage('Package'){
      try {
        if (context.config.veracode?.stashname)
        {
          echo "re-using existing stash"
        }
        else
        {
          echo "building new stash"
          deployer.build("${targetBranch}", context, pathToBuildScript)
        }
      } catch(error) {
        echo error.message
        echo "IGNORE: Failure"
      }
    }
    milestone(label:'packaged')

    lock(inversePrecedence: true, quantity: 1, resource: veracodeApiLock){
    	stage('Veracode'){
          node(){
      		deployer.uploadVeracode("${targetBranch}", context)
          }
    	}
    	milestone(label:'uploaded')
    } //End Lock
  } catch (error) {
    currentBuild.result = 'FAILURE'
    deployer.emailVeracodeFail("${targetBranch}", context)
    throw error
  }
}
