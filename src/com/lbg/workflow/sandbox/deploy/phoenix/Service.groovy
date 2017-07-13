package com.lbg.workflow.sandbox.deploy.phoenix

class Service implements Serializable {

    /**
     * service name
     */
    String name

    /**
     * build pack
     */
    String buildpack

    /**
     * IBM WAS Version for UCD MCA Deployment
     */
    String wasVersion

    /**
     * deployment choice
     */
    boolean deploy

    /**
     * upload choice
     */
    boolean upload

    /**
     * onlyChanged choice
     */
    boolean onlyChanged

    /**
     * deployment type
     */
    String type

    /**
     * service runtime
     */
    ServiceRuntime runtime

    /**
     * list of components to deploy
     */
    List<Components> components = new ArrayList<>()

    /**
     * proxy config
     */
    HashMap proxy

    /**
     * service specific env
     */
    HashMap env

    /**
     * deployment env
     */
    HashMap deployment

    /**
     * Service Description
     */
    String description

    Service() {
    }

    @Override
    String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", wasVersion='" + wasVersion + '\'' +
                ", buildpack=" + buildpack+
                ", deploy=" + deploy +
                ", upload=" + upload +
                ", onlyChanged=" + onlyChanged +
                ", type=" + type +
                ", runtime=" + runtime +
                ", proxy=" + proxy +
                ", env=" + env +
                ", deployment=" + deployment +
                ", components=" + components +
                '}'
    }
}
