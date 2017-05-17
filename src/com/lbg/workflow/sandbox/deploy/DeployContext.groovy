package com.lbg.workflow.sandbox.deploy

import groovy.json.JsonSlurperClassic

class DeployContext implements Serializable {

    /**
     * deployment journey
     */
    String journey

    /**
     * deployment environment - test, qa, stage
     */
    String env

    /**
     * deployment target - bluemix or urbancode?
     */
    String target

    /**
     * deployment metadata
     */
    HashMap metadata

    /**
     * list of services to deploy
     */
    List<Service> services = new ArrayList<>()

    /**
     * deployment bluemix config
     */
    HashMap bluemix

    /**
     * deployment proxy config
     */
    HashMap proxy

    DeployContext() {
    }

    DeployContext(String configuration) {
        def config = (new HashMap(new JsonSlurperClassic().parseText(configuration))).asImmutable()
        this.journey = config.journey
        this.env = config.env
        this.target = config.target
        this.metadata = config.metadata
        this.bluemix = config.bluemix
        this.proxy = config.proxy
        def dc = new DeployContext(config) // avoid lazymap issues
        this.services = dc.services
    }

    @Override
    String toString() {
        return "DeployContext{" +
                "journey='" + journey + '\'' +
                ", env='" + env + '\'' +
                ", target='" + target + '\'' +
                ", metadata='" + metadata + '\'' +
                ", services=" + services +
                ", bluemix=" + bluemix +
                ", proxy=" + proxy +
                '}'
    }
}