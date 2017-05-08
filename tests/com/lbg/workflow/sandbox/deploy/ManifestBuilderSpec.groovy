package com.lbg.workflow.sandbox.deploy

import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

class ManifestBuilderSpec extends Specification {

    def "build manifest for a given service"() {
        given:
        def appName = "test-bluemix-job-test"
        def envs = ["NODE_ENV": "test", "ENTERPRISE": "lbg"]
        def serviceBluemix = [
                "disk": "512M",
                "memory": "512M"
        ]
        Service service = new Service()
        service.env = envs
        service.bluemix = serviceBluemix

        DeployContext deployContext = new DeployContext()
        def deployContextBluemix = [
                "domain": "lbg.eu-gb.mybluemix.net",
                "api": "api.lbg.eu-gb.bluemix.net",
                "credentials": "bluemix-global-deployer",
                "org": "POC35_PSD2AISP",
                "env": "Test",
                "disk": "256M",
                "memory": "256M"
        ]
        deployContext.bluemix = deployContextBluemix

        when:
        ManifestBuilder manifestBuilder = new ManifestBuilder()
        def manifest = manifestBuilder.build(appName, service, deployContext)

        then:
        Yaml parser = new Yaml()
        def yamlResult = parser.load(manifest)
        assert yamlResult.applications.name == [appName]
        assert yamlResult.applications.org == [deployContextBluemix.org]
        assert yamlResult.applications.space == [deployContextBluemix.env]
        assert yamlResult.applications.disk_quota == [serviceBluemix.disk]
        assert yamlResult.applications.memory == [serviceBluemix.memory]
        assert yamlResult.applications.env[0]["NODE_MODULES_CACHE"] == false
        assert yamlResult.applications.env[0]["ENTERPRISE"] == "lbg"
        assert yamlResult.applications.env[0]['NODE_ENV'] == "test"
    }
}
