package vars

import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.ServiceRuntime
import com.lbg.workflow.sandbox.deploy.ServiceRuntimeBinary
import com.lesfurets.jenkins.unit.MethodSignature
import support.PipelineSpockTestBase

class eagleDeployApiConnectServiceSpec extends PipelineSpockTestBase {

    def "deploy given service"() {
        given:
        Service service = new Service()
        service.name = "service-name"
        def artifact = "https://nexus/artifact-9-deb9b7e.tar.gz"
        ServiceRuntimeBinary binary = new ServiceRuntimeBinary()
        binary.artifact = artifact
        ServiceRuntime runtime = new ServiceRuntime()
        runtime.binary = binary
        service.runtime = runtime
        service.type = "Node.js"
        service.env = [:]
        service.env["NPM_SETUP_TASK"] = "publish"

        DeployContext deployContext = new DeployContext()
        deployContext.platforms.target = "apiconnect"
        deployContext.platforms.apiconnect = [:]
        deployContext.platforms.apiconnect["server"] = "management01.psd2.sandbox.extranet.group"
        deployContext.platforms.apiconnect["credentials"] = "bluemix-global-deployer"
        deployContext.platforms.apiconnect["org"] = "cma-phase-2-test"


        helper.registerAllowedMethod(MethodSignature.method("fileExists", String.class), {
            return true
        })

        when:
        def script = loadScript("vars/eagleDeployApiConnectService.groovy")
        def result = script.call(service, deployContext)

        then:
        printCallStack()
        assertJobStatusSuccess()
    }
}