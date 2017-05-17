package vars

import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.ServiceRuntime
import com.lbg.workflow.sandbox.deploy.ServiceRuntimeBinary
import support.PipelineSpockTestBase

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat

class eagleDeployTesterSpec extends PipelineSpockTestBase {

    def "ping test deployed service and proxy for the given deploy context"() {
        given:
        Service service = new Service()
        service.name = "service-name"
        service.deploy = true
        DeployContext deployContext = new DeployContext()
        deployContext.journey = "journey"
        deployContext.env = "env"
        deployContext.target = "bluemix"
        deployContext.bluemix = [:]
        deployContext.bluemix['domain'] = 'domain'
        deployContext.services = [service]
        deployContext.proxy = ["deploy": true]
        helper.registerAllowedMethod("pingTester", [String.class], null)

        when:
        def script = loadScript("vars/eagleDeployTester.groovy")
        script.call(deployContext)

        then:
        printCallStack()
        assertJobStatusSuccess()

        helper.methodCallCount("pingTester") == 2
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "pingTester"
        }.any {
            call ->
                call.args == ["journey-service-name-env.domain"] ||
                        call.args == ["journey-proxy-env.domain"]
        }).isTrue()
    }

    def "raise error if ping test fails for service"() {
        given:
        Service service = new Service()
        service.name = "service-name"
        service.deploy = true
        DeployContext deployContext = new DeployContext()
        deployContext.journey = "journey"
        deployContext.env = "env"
        deployContext.target = "bluemix"
        deployContext.bluemix = [:]
        deployContext.bluemix['domain'] = 'domain'
        deployContext.services = [service]
        deployContext.proxy = ["deploy": true]
        helper.registerAllowedMethod("pingTester", [String.class], { url ->
            if (url == "journey-service-name-env.domain") {
                throw new Exception("failed to test service")
            }
        })

        when:
        def script = loadScript("vars/eagleDeployTester.groovy")
        script.call(deployContext)

        then:
        printCallStack()
        assertJobStatusFailure()

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "pingTester"
        }.any {
            call ->
                call.args == ["journey-service-name-env.domain"]
        }).isTrue()
        Exception exception = thrown()
        exception.message == "failed to test service"
    }

    def "raise error if ping test fails for proxy"() {
        given:
        Service service = new Service()
        service.name = "service-name"
        service.deploy = true
        DeployContext deployContext = new DeployContext()
        deployContext.journey = "journey"
        deployContext.env = "env"
        deployContext.target = "bluemix"
        deployContext.bluemix = [:]
        deployContext.bluemix['domain'] = 'domain'
        deployContext.services = [service]
        deployContext.proxy = ["deploy": true]
        helper.registerAllowedMethod("pingTester", [String.class], { url ->
            if (url == "journey-proxy-env.domain") {
                throw new Exception("failed to test proxy")
            }
        })

        when:
        def script = loadScript("vars/eagleDeployTester.groovy")
        script.call(deployContext)

        then:
        printCallStack()
        assertJobStatusFailure()

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "pingTester"
        }.any {
            call ->
                call.args == ["journey-service-name-env.domain"] ||
                        call.args == ["journey-proxy-env.domain"]
        }).isTrue()
        Exception exception = thrown()
        exception.message == "failed to test proxy"
    }
}