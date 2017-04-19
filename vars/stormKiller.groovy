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
    def stormTopologyName = config.stormTopName
    def sshOpts = '-o StrictHostKeyChecking=no'


    println "[Library] Stopping ${stormTopologyName} on  ${stormNode} ..."
    sshagent(credentials: [stormSSHUSer]) {
        sh """
          ssh ${sshOpts} ${stormRemoteUser}@${stormNode} "storm kill ${stormTopologyName} || true"
        """
    }

}


String name() {
    return "Storm topology killer"
}

return this;