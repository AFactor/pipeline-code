package com.lbg.workflow.sandbox.deploy.phoenix

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
     * deployment restrict to label / node(s)
     */
    String label

    /**
     * deployment metadata
     */
    HashMap metadata

    /**
     * deployment information
     */
    HashMap deployment

    /**
     * list of services to deploy
     */
    List<Service> services = new ArrayList<>()

    /**
     * list of components to deploy
     */
    List<Components> components = new ArrayList<>()

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
        this.label = config.label
        this.deployment = config.deployment
        this.metadata = config.metadata
        this.proxy = config.proxy
        def dc = new DeployContext(config) // avoid lazymap issues
        this.services = dc.services
        this.components = dc.services.components
    }

    /* unused
    @NonCPS
    static def parseJsonText(String json) {
        def object = new JsonSlurper().parseText(json)
          if(object instanceof groovy.json.internal.LazyMap) {
              return new HashMap<>(object)
          }
          return object
    } */

    @Override
    String toString() {
        return "DeployContext{" +
                "journey='" + journey + '\'' +
                ", env='" + env + '\'' +
                ", label='" + label + '\'' +
                ", metadata='" + metadata + '\'' +
                ", services=" + services +
                ", deployment=" + deployment +
                ", components=" + components +
                ", proxy=" + proxy +
                '}'
    }
}