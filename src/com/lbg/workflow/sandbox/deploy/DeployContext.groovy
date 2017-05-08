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
        this.bluemix = config.bluemix
        this.proxy = config.proxy
        def dc = new DeployContext(config) // avoid lazymap issues
        this.services = dc.services

        //validate()
    }

    private def validate() {

        if (this.journey == null) {
            throw new Exception("Invalid Configuration - journey must be defined")
        }
        if (this.env == null) {
            throw new Exception("Invalid Configuration - env must be defined")
        }
        if (this.target == null) {
            throw new Exception("Invalid Configuration - target must be defined")
        }
        if (this.services == null) {
            throw new Exception("Invalid Configuration - services must be defined")
        }
        if (this.proxy == null) {
            throw new Exception("Invalid Configuration - proxy config must be defined")
        }
        if (this.bluemix == null) {
            throw new Exception("Invalid Configuration - bluemix config must be defined")
        }
    }

    @Override
    String toString() {
        return "DeployContext{" +
                "journey='" + journey + '\'' +
                ", env='" + env + '\'' +
                ", target='" + target + '\'' +
                ", services=" + services +
                ", bluemix=" + bluemix +
                ", proxy=" + proxy +
                '}'
    }
}