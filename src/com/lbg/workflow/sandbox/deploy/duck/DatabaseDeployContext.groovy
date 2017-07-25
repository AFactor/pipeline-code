package com.lbg.workflow.sandbox.deploy.duck

class DatabaseDeployContext implements Serializable{

    final HashMap config
    final String application

    DatabaseDeployContext(String configuration) {
        this.application = 'default'
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        )
        ).asImmutable()
    }
    DatabaseDeployContext(String application, String configuration) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        )
        ).asImmutable()
    }
}
