package com.lbg.workflow

import com.lesfurets.jenkins.unit.MethodSignature
import support.PipelineSpockTestBase

class createEagleContextSpec extends PipelineSpockTestBase {

    def "build manifest"() {
        given:
        String appName = "app-name"
        def release = "tests/resources/release/release.json"
        def services = "tests/resources/services/services.json"
        def serviceTokens = "tests/resources/services/tokens.json"
        def platforms =  "tests/resources/platforms/platforms.json"
        def platformTokens =  "tests/resources/platforms/tokens.json"
        helper.registerAllowedMethod(MethodSignature.method("readFile", String.class), { file ->
            return new File(file).text
        })

        when:
        def script = loadScript("vars/createEagleContext.groovy")
        def result = script.call(release, services, serviceTokens, platforms, platformTokens)

        then:
        printCallStack()
        assertJobStatusSuccess()
        assert result != null

        println result
    }
}