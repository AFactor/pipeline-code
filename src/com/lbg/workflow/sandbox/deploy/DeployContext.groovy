package com.lbg.workflow.sandbox.deploy

class DeployContext implements Serializable {

    // release info
    HashMap release

    // services - !!! do not be tricked, this is not a list of Service objects, but a list of HashMap !!!
    List<Service> services = new ArrayList<>()

    // platforms
    HashMap platforms

    DeployContext() {
    }

    String releaseVersion() {
        "${release.version.major}.${release.version.minor}.${release.version.patch}"
    }

    @Override
    String toString() {
        return "DeployContext{" +
                "release=" + release +
                ", services=" + services +
                ", platforms=" + platforms +
                '}'
    }
}
