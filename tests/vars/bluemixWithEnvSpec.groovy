package vars

import support.PipelineSpockTestBase

class bluemixWithEnvSpec extends PipelineSpockTestBase {

    def "build environment variable for given bluemix config"() {
        given:
        HashMap bluemix = [
                "org"        : "org-name",
                "domain"     : "lbg.eu-gb.mybluemix.net",
                "api"        : "api.lbg.eu-gb.bluemix.net",
                "credentials": "bluemix-global-deployer",
                "env"        : "Test",
                "disk"       : "256M",
                "memory"     : "256M"]

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

    def "build environment variables for given service and global bluemix config"() {
        given:
        HashMap serviceBluemixConfig = ["deployable": "j2/service-name", "APP": "service-name"]
        HashMap globalBluemixConfig = [
                "org"        : "org-name",
                "domain"     : "lbg.eu-gb.mybluemix.net",
                "api"        : "api.lbg.eu-gb.globalBluemixConfig.net",
                "credentials": "globalBluemixConfig-global-deployer",
                "env"        : "Test",
                "disk"       : "256M",
                "memory"     : "256M"]

        when:
        def script = loadScript("vars/bluemixWithEnv.groovy")
        def result = script.call(serviceBluemixConfig, globalBluemixConfig)

        then:
        printCallStack()
        assertJobStatusSuccess()
        assert result != null
        assert result == [
                "BM_API=${globalBluemixConfig.api}",
                "BM_DOMAIN=${globalBluemixConfig.domain}",
                "BM_ORG=${globalBluemixConfig.org}",
                "BM_ENV=${globalBluemixConfig.env}",
                "DISK=${globalBluemixConfig.disk}",
                "MEMORY=${globalBluemixConfig.memory}",
                "deployable=j2/service-name",
                "APP=service-name"
        ]
    }
}