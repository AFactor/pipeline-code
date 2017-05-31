package vars

import com.lbg.workflow.sandbox.deploy.DeployContext
import com.lbg.workflow.sandbox.deploy.Service
import com.lbg.workflow.sandbox.deploy.ServiceRuntime
import com.lbg.workflow.sandbox.deploy.ServiceRuntimeBinary
import com.lesfurets.jenkins.unit.MethodSignature
import groovy.json.JsonOutput
import org.codehaus.groovy.runtime.GStringImpl
import support.PipelineSpockTestBase

class invokeDeployPipelineEagleSpec extends PipelineSpockTestBase {

    def setup() {
        addEnvVar('CONFLUENCE_CREDENTIALS', "CONFLUENCE_CREDENTIALS")
    }

    def "deploy pipeline eagle"() {
        given:
        def configuration = "pipelines/conf/job-configuration.json"
        helper.registerAllowedMethod(MethodSignature.method("readFile", String.class), { file ->
            if (file == configuration) {

                DeployContext deployContext = new DeployContext()
                deployContext.journey = "journey"
                deployContext.target = "target"
                deployContext.env = "env"
                deployContext.target = "bluemix"
                Map confluence = [:]
                confluence["server"] = "server"
                confluence["page"] = "page"
                deployContext.metadata = ["notifyList":"email", "confluence": confluence]
                Service service = new Service()
                service.name = "service-name"
                def artifact = "http://nexus/artifact-9-deb9b7e.tar.gz"
                ServiceRuntimeBinary binary = new ServiceRuntimeBinary()
                binary.artifact = artifact
                ServiceRuntime runtime = new ServiceRuntime()
                runtime.binary = binary
                service.runtime = runtime
                deployContext.services = [service]
                deployContext.bluemix = [:]
                deployContext.bluemix['domain'] = 'domain'
                deployContext.proxy = [:]
                deployContext.proxy['deploy'] = true

                def json = JsonOutput.toJson(deployContext)
                println json
                return json
            }
        })

        helper.registerAllowedMethod("eagleDeployProxy", [DeployContext.class], {})
        helper.registerAllowedMethod("eagleDeployTester", [DeployContext.class], {})
        helper.registerAllowedMethod(MethodSignature.method("emailNotify", Closure.class), {})

        helper.registerAllowedMethod("usernameColonPassword", [HashMap.class], {})
        helper.registerAllowedMethod("confluencePublisher", [String.class, String.class, String.class, String.class], null)
        helper.registerAllowedMethod("jiraPublisher", [String.class, String.class], {})

        when:
        def script = loadScript("vars/invokeDeployPipelineEagle.groovy")
        script.call(configuration)

        then:
        printCallStack()
        assertJobStatusSuccess()
    }



    def "deploy pipeline must fail on invalid configuration "() {
        given:
        def configuration = "pipelines/conf/job-configuration.json"
        helper.registerAllowedMethod(MethodSignature.method("readFile", String.class), { file ->
            if (file == configuration) {
                return "{}"
            }
        })

        helper.registerAllowedMethod("eagleDeployTester", [DeployContext.class], null)
        helper.registerAllowedMethod(MethodSignature.method("emailNotify", String.class), {})

        when:
        def script = loadScript("vars/invokeDeployPipelineEagle.groovy")
        script.call(configuration)

        then:
        printCallStack()
        assertJobStatusFailure()
    }
}
