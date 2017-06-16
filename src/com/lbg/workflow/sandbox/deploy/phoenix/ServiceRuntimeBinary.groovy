package com.lbg.workflow.sandbox.deploy.phoenix

class ServiceRuntimeBinary implements Serializable  {

    String nexus_url
    String version
    String revision
    String extension
    String name
    String artifactName
    String artifact
    String branch

    @Override
    String toString() {
        return "ServiceRuntimeBinary{" +
                "artifact='" + artifact + '\'' +
                "git_sha(revision)='" + revision + '\'' +
                "version='" + version + '\'' +
                "git_branch(branch)='" + branch + '\'' +
                "extension='" + extension + '\'' +
                "name='" + artifactName + '\'' +
                '}'
    }
}
