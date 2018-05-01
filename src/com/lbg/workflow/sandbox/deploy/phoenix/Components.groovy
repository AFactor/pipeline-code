package com.lbg.workflow.sandbox.deploy.phoenix

class Components implements Serializable {
    /**
     * component base directory
     */
    String baseDir

    /**
     * component name
     */
    String name

    /**
     * component description
     */
    String description

    /**
     * component version
     */
    String version

    /**
     * component revision
     */
    String revision

    /**
     * component version path
     */
    String versionPath

    Components() {
    }

    @Override
    String toString() {
        return "Components{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", baseDir='" + description + '\'' +
                ", version='" + version + '\'' +
                ", revision='" + revision + '\'' +
                ", versionPath='" + versionPath + '\'' +
                '}'
    }
}
