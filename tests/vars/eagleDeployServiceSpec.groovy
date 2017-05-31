package com.lbg.workflow

import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.ServiceRuntime
import com.lbg.workflow.sandbox.deploy.ServiceRuntimeBinary
import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.MethodSignature
import support.PipelineSpockTestBase
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat


class eagleDeployServiceSpec extends PipelineSpockTestBase {

    def "deploy given service"() {
        given:
        Service service = new Service()
        service.name = "service-name"
        def artifact = "http://nexus/artifact-9-deb9b7e.tar.gz"
        ServiceRuntimeBinary binary = new ServiceRuntimeBinary()
        binary.artifact = artifact
        ServiceRuntime runtime = new ServiceRuntime()
        runtime.binary = binary
        service.runtime = runtime
        service.buildpack = "Node.js"

        DeployContext deployContext = new DeployContext()
        deployContext.target = "bluemix"
        helper.registerAllowedMethod("eagleDeployBluemixService", [Service.class, DeployContext.class], null)

        when:
        def script = loadScript("vars/eagleDeployService.groovy")
        def result = script.call(service, deployContext)

        then:
        printCallStack()
        assertJobStatusSuccess()

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "sh"
        }.any {
            call -> callArgsToString(call)
                    .equals("mkdir -p service-name && \\\n" +
                    "                  wget --quiet http://nexus/artifact-9-deb9b7e.tar.gz && \\\n" +
                    "                  tar -xf artifact-9-deb9b7e.tar.gz -C service-name")
        }).isTrue()


        assertThat(helper.callStack.findAll { call ->
            call.methodName == "eagleDeployBluemixService"
        }.any {
            call -> call.args == [service, deployContext]
        }).isTrue()
    }
}