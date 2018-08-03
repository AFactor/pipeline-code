#!/usr/local/bin/groovy
import com.lbg.workflow.sandbox.*

def call(String appName, pathToConfig)
{
  def vcd
  def context
  def epoch
  def targetBranch
  def veracodeCredentials
  try {
    stage('Initialize'){
        node(){
            deleteDir()
            checkout scm
            targetBranch = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
            context = new BuildContext(appName, readFile(pathToConfig))
            epoch = sh(returnStdout: true, script: "date +%d%m%Y%H%M").trim()
            veracodeCredentials = context.config.veracode.credentials
            vcd = new veracode()
            echo "Loaded"
        }
    }
    milestone(label:'initialized')

    lock(inversePrecedence: true, quantity: 1, resource: veracodeCredentials){
        stage('Report'){
        node(){
            vcd.download("${targetBranch}", context)
            }
        }
        milestone(label:'downloaded')

      stage('Splunk'){
        node(){
          try {
              vcd.publishSplunk(targetBranch, epoch, context)
          } catch (error) {
              print "Failed publishing reports to splunk. Continuing."
          }
        }
      }
      milestone(label:'Splunk')
    } //End Lock

    stage('Email'){
        node('master'){
            vcd.email("${targetBranch}", context)
        }
    }
    milestone(label:'Email')

  } catch(error){
      currentBuild.result = 'FAILURE'
      vcd.emailFail("${targetBranch}", context)
      throw error
    }
}
