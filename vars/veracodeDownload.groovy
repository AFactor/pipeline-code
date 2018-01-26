#!/usr/local/bin/groovy

import com.lbg.workflow.sandbox.*


def call(String appName, pathToConfig)
{
  def deployer
  def context
  def tester
  def epoch
  def targetBranch
  def veracodeApiCode
  try {
    stage('Initialize'){
    	node(){
    		deleteDir()
    		checkout scm
    		targetBranch = env.BRANCH_NAME
          	context = new BuildContext(appName, readFile(pathToConfig))
    		epoch = sh(returnStdout: true, script: "date +%d%m%Y%H%M").trim()
                veracodeApiCode = context.config.veracode.id
    		deployer = new veracode()
    		echo "Loaded"
    	}
    }
    milestone(label:'initialized')

    lock(inversePrecedence: true, quantity: 1, resource: veracodeApiCode){
    	stage('Report'){
      	node(){
      		deployer.downloadVeracode("${targetBranch}", context)
    		}
    	}
    	milestone(label:'downloaded')

      stage('Splunk'){
        node(){
          deployer.publishSplunk(targetBranch, epoch, context)
        }
      }
      milestone(label:'Splunk')
    } //End Lock

    stage('Email'){
    	node('master'){
    		deployer.emailVeracode("${targetBranch}", context)
    	}
    }
    milestone(label:'Email')
  } catch(error)
    { 
      currentBuild.result = 'FAILURE'
      deployer.emailVeracodeFail("${targetBranch}", context)
      throw error
    }
}
