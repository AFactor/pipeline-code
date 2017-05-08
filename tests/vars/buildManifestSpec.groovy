package com.lbg.workflow

import org.yaml.snakeyaml.Yaml
import support.PipelineSpockTestBase

class buildManifestSpec extends PipelineSpockTestBase {

    def "build manifest"() {
        given:
        String appName = "app-name"
        HashMap env = ["NODE_ENV": "test", "ENTERPRISE": "lbg"]
        HashMap bluemix = [
                "org": "org-name",
                "domain": "lbg.eu-gb.mybluemix.net",
                "api": "api.lbg.eu-gb.bluemix.net",
                "credentials": "bluemix-global-deployer",
                "env": "Test",
                "disk": "256M",
                "memory": "256M"]

        when:
        def script = loadScript("vars/buildManifest.groovy")
        def result = script.call(appName, env, bluemix)


        then:
        printCallStack()
        assertJobStatusSuccess()
        assert result != null
        Yaml parser = new Yaml()
        def yamlResult = parser.load(result)
        assert yamlResult.applications.name == [appName]
        assert yamlResult.applications.org == [bluemix.org]
        assert yamlResult.applications.space == [bluemix.env]
        assert yamlResult.applications.disk_quota == [bluemix.disk]
        assert yamlResult.applications.memory == [bluemix.memory]
        assert yamlResult.applications.env[0]["NODE_MODULES_CACHE"] == false
        assert yamlResult.applications.env[0]["ENTERPRISE"] == "lbg"
        assert yamlResult.applications.env[0]['NODE_ENV'] == "test"
    }
}