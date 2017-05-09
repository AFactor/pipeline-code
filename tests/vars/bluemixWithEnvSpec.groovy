package vars

import support.PipelineSpockTestBase

class bluemixWithEnvSpec extends PipelineSpockTestBase {

    def "build bluemix withenv configuration"() {
        given:
        HashMap bluemix = [
                "org": "org-name",
                "domain": "lbg.eu-gb.mybluemix.net",
                "api": "api.lbg.eu-gb.bluemix.net",
                "credentials": "bluemix-global-deployer",
                "env": "Test",
                "disk": "256M",
                "memory": "256M"]

        when:
        def script = loadScript("vars/bluemixWithEnv.groovy")
        def result = script.call(bluemix)

        then:
        printCallStack()
        assertJobStatusSuccess()
        assert result != null
        assert result == [
        "BM_API=${bluemix.api}",
        "BM_DOMAIN=${bluemix.domain}",
        "BM_ORG=${bluemix.org}",
        "BM_ENV=${bluemix.env}",
        "DISK=${bluemix.disk}",
        "MEMORY=${bluemix.memory}"
        ]
    }
}