package com.lbg.workflow.sandbox.deploy

import spock.lang.Specification

class ServiceWrapperSpec extends Specification {

    def "throws runtime exception when artifact is null"() {
        given:
        def serviceHash = ["runtime": ["binary": ["artifact": null]]]

        when:
        new ServiceWrapper(serviceHash)

        then:
        final RuntimeException exception = thrown()
        exception.message == "service.runtime.binary.artifact null or empty !!!"
    }

    def "throws runtime exception when artifact is blank"() {
        given:
        def serviceHash = ["runtime": ["binary": ["artifact": ""]]]

        when:
        new ServiceWrapper(serviceHash)

        then:
        final RuntimeException exception = thrown()
        exception.message == "service.runtime.binary.artifact null or empty !!!"
    }

    def "throws runtime exception when component_name is null"() {
        given:
        def serviceHash = ["runtime": ["binary": ["artifact": "some_url"]], "platforms": ["ucd": ["component_name": null]]]

        when:
        new ServiceWrapper(serviceHash)

        then:
        final RuntimeException exception = thrown()
        exception.message == "service.platforms.ucd.component_name null or empty !!!"
    }

    def "throws runtime exception when component_name is emtpy"() {
        given:
        def serviceHash = ["runtime": ["binary": ["artifact": "some_url"]], "platforms": ["ucd": ["component_name": ""]]]

        when:
        new ServiceWrapper(serviceHash)

        then:
        final RuntimeException exception = thrown()
        exception.message == "service.platforms.ucd.component_name null or empty !!!"
    }

    def "returns a service wrapper"() {
        given:
        def nexusUrl = "http://nexus.sandbox.extranet.group/nexus/content/repositories/releases/com/lbg/ob/aisp/agreements-channel-service/1.0.0-15.1.210c1ca/agreements-channel-service-1.0.0-15.1.210c1ca.tar.gz"
        def componentName = "Digital-OB-C-aisp-agreements-channel-service-nodejs"
        def serviceHash = ["runtime"  : ["binary": ["artifact": nexusUrl]],
                           "platforms": ["ucd": ["component_name": componentName]]]

        when:
        def wrappedService = new ServiceWrapper(serviceHash)

        then:
        wrappedService.nexusUrl() == nexusUrl
        wrappedService.componentName() == componentName
        wrappedService.artifactSaveAsName() == "agreements-channel-service-1.0.0-15.1.210c1ca.tar.gz"
        wrappedService.componentVersion() == "1.0.0-15.1.210c1ca"
    }

}
