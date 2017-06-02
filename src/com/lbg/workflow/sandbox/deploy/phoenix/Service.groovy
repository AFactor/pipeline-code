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
     * deployment choice
     */
    boolean deploy

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
                ", buildpack=" + buildpack+
                ", deploy=" + deploy +
                ", type=" + type +
                ", runtime=" + runtime +
                ", proxy=" + proxy +
                ", env=" + env +
                ", deployment=" + deployment +
                ", components=" + components +
                '}'
    }
}
