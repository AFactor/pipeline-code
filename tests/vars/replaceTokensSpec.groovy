package vars

import support.PipelineSpockTestBase

class replaceTokensSpec extends PipelineSpockTestBase {

    def "replace Tokens"() {
        given:
        String targetDirectory = "test"
        HashMap env = [
                "NODE_ENV": "test",
                "NODE_MODULES_CACHE": "false",
                "ENTERPRISE": "lbg",
                "BASE_URL": "http://base.url/base?first=1&second=2",
                "VALIDATE_UPSTREAM_RESPONSE": "false",
                "REQUEST_TIMEOUT_IN_MILLISECONDS": "1",
                "OPEN_CIRCUIT_TIMEOUT_IN_MILLISECONDS": "5000"
        ]

        when:
        def script = loadScript("vars/replaceTokens.groovy")
        def result = script.call(targetDirectory, env)

        then:
        printCallStack()
        assertJobStatusSuccess()
    }
}