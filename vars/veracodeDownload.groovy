#!/usr/local/bin/groovy
import com.lbg.workflow.sandbox.*

def call(String appName, pathToConfig)
{
  def deployer
  def context
  def epoch
  def targetBranch
  def veracodeAppId
  try {
    stage('Initialize'){
        node(){
            deleteDir()
            checkout scm
            targetBranch = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
            context = new BuildContext(appName, readFile(pathToConfig))
            epoch = sh(returnStdout: true, script: "date +%d%m%Y%H%M").trim()
            veracodeAppId = context.config.veracode.id
            deployer = new veracode()
            echo "Loaded"
        }
    }
    milestone(label:'initialized')

    lock(inversePrecedence: true, quantity: 1, resource: veracodeAppId){
        stage('Report'){
        node(){
            deployer.downloadVeracode("${targetBranch}", context)
            }
        }
        milestone(label:'downloaded')

      stage('Splunk'){
        node(){
          try {
              deployer.publishSplunk(targetBranch, epoch, context)
          } catch (error) {
              print "Failed publishing reports to splunk. Continuing."
          }
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

  } catch(error){
      currentBuild.result = 'FAILURE'
      deployer.emailVeracodeFail("${targetBranch}", context)
      throw error
    }
}
