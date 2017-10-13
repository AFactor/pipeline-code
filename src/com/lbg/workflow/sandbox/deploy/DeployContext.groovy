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
     * cmc config
     */
    HashMap cmc

    /**
     * deployment proxy config
     */
    HashMap proxy

    /**
     * ucd config
     */
    HashMap ucd

    /**
     * deployment restrict to label / node(s)
     */
    String label

    /**
     * deployment env
     */
    HashMap deployment

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
        this.cmc = config.cmc
        this.proxy = config.proxy
        this.schema = config.schema
        this.ucd = config.ucd
        this.deployment = config.deployment
        this.label = config.label

        def dc = new DeployContext(config) // avoid jenkins serialization issues
        this.services = dc.services

        // Unfortunately this does not work - getting jenkins serialization errors
        // leaving the code for now, hopefully when we upgrade our jenkins, we can revisit this.
        // manually parse services & binaries
//        ArrayList services = new ArrayList()
//        for (def s in config.services) {
//            Service service = new Service(s)
//            services.add(service)
//        }
//        this.services = services
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
                ", cmc=" + cmc +
                ", deployment=" + deployment +
                ", proxy=" + proxy +
                '}'
    }
}
