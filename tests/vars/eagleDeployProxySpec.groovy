package vars

import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.ServiceRuntime
import com.lbg.workflow.sandbox.deploy.ServiceRuntimeBinary
import com.lesfurets.jenkins.unit.MethodSignature
import support.PipelineSpockTestBase

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat

class eagleDeployProxySpec extends PipelineSpockTestBase {

    def "deploy given service"() {
        given:
        DeployContext deployContext = new DeployContext()
        deployContext.release.journey= "journey"
        deployContext.release.environment = "test"
        deployContext.platforms.bluemix = [:]
        deployContext.platforms.bluemix['domain'] = 'domain'
        Service service = new Service()
        service.name = "service-name"
        service.proxy = ["/service-target": "/service-source"]
        deployContext.services = [service]

        helper.registerAllowedMethod(MethodSignature.method("readFile", String.class), { file ->
            if (file.equals("nginx/nginx.conf.head")) {
                return "nginx-head"
            }
            if (file.equals("nginx/nginx.conf.tail")) {
                return "nginx-tail"
            }
            return null
        })
        helper.registerAllowedMethod(MethodSignature.method("archiveArtifacts", String.class), {})
        helper.registerAllowedMethod("eagleDeployBluemixProxy", [String.class, DeployContext.class], null)

        when:
        def script = loadScript("vars/eagleDeployProxy.groovy")
        script.call(deployContext)

        then:
        printCallStack()
        assertJobStatusSuccess()

        // read nginx header and footer
        assert helper.methodCallCount("readFile") == 2

        // mkdir nginx
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "sh"
        }.any { call ->
            callArgsToString(call).contains("mkdir -p journey-proxy-test")
        }).isTrue()

        // chdir nginx
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "dir"
        }.any { call ->
            callArgsToString(call).contains("journey-proxy-test")
        }).isTrue()

        // write nginx
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "writeFile"
        }.any { call ->
            call.args[0].text.contains("location /service-source") &&
                    call.args[0].text.contains("proxy_pass https://journey-service-name-test.domain/service-target")
        }).isTrue()

        // archive nginx
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "archiveArtifacts"
        }.any { call ->
            call.args == ["nginx.conf"]
        }).isTrue()

        // invoke eagleDeployBluemixProxy
        assertThat(helper.callStack.findAll { call ->
            call.methodName == "eagleDeployBluemixProxy"
        }.any { call ->
            call.args == ["journey-proxy-test", deployContext]
        }).isTrue()

    }
}