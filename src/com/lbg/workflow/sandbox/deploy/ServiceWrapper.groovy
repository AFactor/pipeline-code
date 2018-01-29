package com.lbg.workflow.sandbox.deploy

/**
 *  placeholder for shared utilities operating on Service (Eagle pipeline)
 */

class ServiceWrapper implements Serializable {

    private def service

    ServiceWrapper(serviceHash) {

        if (!serviceHash?.runtime?.binary?.artifact?.trim())
            throw new RuntimeException("service.runtime.binary.artifact null or empty !!!")

        if (!serviceHash?.platforms?.ucd?.component_name?.trim())
            throw new RuntimeException("service.platforms.ucd.component_name null or empty !!!")

        this.service = serviceHash
    }

    String artifactSaveAsName() {
        artifactName()
    }

    String componentName() {
        service.platforms.ucd.component_name
    }

    String componentVersion() {
        artifactVersion()
    }

    String nexusUrl() {
        service.runtime.binary.artifact
    }

    private String artifactVersion() {
        def artifactName = artifactName()

        def m = artifactName =~ /^([\d|\a|-]+)(.*)(\.zip|\.tar\.gz)$/
        if (m) {
            return m[0][2]
        } else {
            // this should never happen
            throw new RuntimeException("artifact name property has a wrong format, should match: ^([\\D|-]+)(.*)(\\.zip|\\.tar\\.gz)\$")
        }
    }

    private String artifactName() {
        def nexusUrl = nexusUrl()
        nexusUrl.substring(nexusUrl.lastIndexOf('/') + 1, nexusUrl.length())
    }
}

