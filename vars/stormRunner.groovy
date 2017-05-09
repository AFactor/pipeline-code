/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.lbg.workflow.sandbox.Utils

def call(Closure body) {

    Utils  utils = new Utils()
    def config = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // configs needed. Defaults pertinent to PAS Big Data Notifications
    def stormNode = config.node
    def stormRemoteUser = config.remoteUser
    def stormSSHUSer = config.sshUser
    def stormTopologyClass = config.stormTopClass
    def stormTopologyName = config.stormTopName
    def sshOpts = '-o StrictHostKeyChecking=no'
    def stormJar= config.jarFileName
    def stormJarPath = config.pathToJar
    def args = config.extraArgs

    println "[Library] Deploying to ${stormJarPath}/${stormJar}, topology ${stormTopologyName} , class, ${stormTopologyClass} to ${stormNode}"
    sshagent(credentials: [stormSSHUSer]) {
        sh """
            ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "storm kill ${stormTopologyName} || true"
            scp ${stormJarPath}/${stormJar} ${stormRemoteUser}@${stormNode}:/tmp
            ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "storm jar /tmp/${stormJar} ${stormTopologyClass} ${args}"
            ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "rm /tmp/${stormJar}"
              """
    }
}


String name() {
    return "Storm topology runner"
}

return this;