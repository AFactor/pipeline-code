package com.lbg.workflow.sandbox.deploy

import groovy.json.JsonSlurperClassic

class DeployContext implements Serializable {

    HashMap schema
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
     * api connect config
     */
    HashMap apiconnect

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
        this.apiconnect = config.apiconnect
        this.proxy = config.proxy
        def dc = new DeployContext(config) // avoid lazymap issues
        this.services = dc.services
        this.schema = dc.schema
    }

    @Override
    String toString() {
        return "DeployContext{" +
                "journey='" + journey + '\'' +
                ", schema='" + schema + '\'' +
                ", env='" + env + '\'' +
                ", target='" + target + '\'' +
                ", metadata='" + metadata + '\'' +
                ", services=" + services +
                ", bluemix=" + bluemix +
                ", apiconnect=" + apiconnect +
                ", proxy=" + proxy +
                '}'
    }
}
