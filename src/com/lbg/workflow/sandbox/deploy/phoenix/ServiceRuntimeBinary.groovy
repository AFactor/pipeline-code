package com.lbg.workflow.sandbox.deploy.phoenix

class ServiceRuntimeBinary implements Serializable  {

    String nexus_url
    String version
    String revision
    String extension
    String name
    String artifactName
    String artifact

    @Override
    String toString() {
        return "ServiceRuntimeBinary{" +
                "artifact='" + artifact + '\'' +
                "revision='" + revision + '\'' +
                "version='" + version + '\'' +
                "extension='" + extension + '\'' +
                "name='" + artifactName + '\'' +
                '}'
    }
}
