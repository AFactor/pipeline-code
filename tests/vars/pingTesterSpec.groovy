package vars

import support.PipelineSpockTestBase

import static org.assertj.core.api.Assertions.assertThat

class pingTesterSpec extends PipelineSpockTestBase {

    def "ping test service with default config"() {
        given:
        String url = "http://service/health"

        when:
        def script = loadScript("vars/pingTester.groovy")
        script.call(script)

        then:
        printCallStack()
        assertJobStatusSuccess()

        assertThat(helper.callStack.findAll { call ->
            call.methodName == "sh"
        }.any { call ->
            call.args = "wget --quiet --wait=60 --tries=3 --spider ${url} && echo 'Success' || echo 'Failure';]"
        }).isTrue()
    }
}