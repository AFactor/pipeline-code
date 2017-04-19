/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

    def config = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // configs needed. Defaults pertinent to PAS Big Data Notifications
    def stormNode = config.node
    def stormRemoteUser = config.remoteUser
    def stormSSHUSer = config.sshUser
    def stormTopologyClass = config.stormTopClass
    def stormJarName = config.pathToJar
    def stormTopologyName = config.stormTopName
    def sshOpts = '-o StrictHostKeyChecking=no'

    println "[Library] Deploying to ${stormTopologyName} (${stormTopologyClass} to ${stormNode}"
    println "[Library] ssh ${stormRemoteUser}@${stormNode} using ${stormSSHUSer} credentials"

    sshagent(credentials: [stormSSHUSer]) {
        sh """
            ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "storm kill ${stormTopologyName } || true"
            ssh ${sshOpts}  ${stormRemoteUser}@${stormNode} "mkdir deployments || true"
            scp ${stormJarName} ${stormRemoteUser}@${stormNode}:deployments
            jar=`ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "ls -t /home/${stormRemoteUser}/deployments/* | head -1"`
            ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "storm jar \$jar ${stormTopologyClass} sbdev cluster"
              """
    }
}


String name() {
    return "Storm topology runner"
}

return this;