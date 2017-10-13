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
     * UCD details
     */
    HashMap ucd

    /**
     * proxy config
     */
    HashMap proxy

    /**
     * service specific env
     */
    HashMap env

	/**
	 * Token specific env
	 */
	HashMap tokens

    /**
     * bluemix env
     */
    HashMap bluemix

    /**
     * deployment env
     */
    HashMap deployment

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
				", tokens=" + tokens +
                ", bluemix=" + bluemix +
                ", ucd=" + ucd +
                ", deployment=" + deployment +
                '}'
    }
}
