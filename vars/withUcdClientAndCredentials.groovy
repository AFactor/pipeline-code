import com.lbg.workflow.sandbox.deploy.UtilsUCD

def call(String ucdUrl, String ucdCredentialsTokenName, Closure closure) {

    if (!fileExists('./udclient/udclient')) {
        new UtilsUCD().install_by_url(ucdUrl)
    }

    // TODO: do not hardcode JAVA_HOME ???
    // at the moment all ucd jobs run on a dedicated LBG node with JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64
    withEnv(['PATH+bin=/bin', 'PATH+usr=/usr/bin', 'PATH+local=/usr/local/bin', 'JAVA_HOME=/usr/lib/jvm/jre-1.7.0-openjdk.x86_64']) {
        withCredentials([string(credentialsId: ucdCredentialsTokenName, variable: 'ucdToken')]) {
            closure(ucdToken)
        }
    }
}
