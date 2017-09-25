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
     * User Input Step -- Should we use the User Input Method or Params Method ??
     *  This is a temporary thing until everything is migrated across
     */
    String user_input_step

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

    /**
     * deployment tests Hashmap
     */
    HashMap tests

    DeployContext() {
    }

    DeployContext(String configuration) {
        def config = (new HashMap(new JsonSlurperClassic().parseText(configuration))).asImmutable()
        this.journey = config.journey
        this.user_input_step = config.user_input_step
        this.env = config.env
        this.label = config.label
        this.deployment = config.deployment
        this.metadata = config.metadata
        this.tests = config.tests
        this.proxy = config.proxy
        def dc = new DeployContext(config) // avoid lazymap issues
        this.services = dc.services
        this.components = dc.services.components
    }

    boolean hasUserInputStep() {
        this.user_input_step == "yes"
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
                ", user_input_step='" + user_input_step + '\'' +
                ", env='" + env + '\'' +
                ", label='" + label + '\'' +
                ", metadata='" + metadata + '\'' +
                ", tests='" + tests + '\'' +
                ", services=" + services +
                ", deployment=" + deployment +
                ", components=" + components +
                ", proxy=" + proxy +
                '}'
    }
}
