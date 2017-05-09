package com.lbg.workflow.sandbox.deploy

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
     * deployment type
     */
    boolean deploy

    /**
     * service runtime
     */
    ServiceRuntime runtime

    /**
     * proxy config
     */
    HashMap proxy

    /**
     * service specific env
     */
    HashMap env

    /**
     * bluemix env
     */
    HashMap bluemix

    Service() {
    }

    @Override
    String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", buildpack=" + buildpack+
                ", deploy=" + deploy +
                ", runtime=" + runtime +
                ", proxy=" + proxy +
                ", env=" + env +
                ", bluemix=" + bluemix +
                '}'
    }
}