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
    String regex

    @Override
    String toString() {
        return "ServiceRuntimeBinary{" +
                "artifact='" + artifact + '\'' +
                ", artifactName='" + artifactName + '\'' +
                ", git_sha(revision)='" + revision + '\'' +
                ", version='" + version + '\'' +
                ", git_branch(branch)='" + branch + '\'' +
                ", extension='" + extension + '\'' +
                ", regex='" + regex + '\'' +
                ", name='" + name + '\'' +
                '}'
    }
}
