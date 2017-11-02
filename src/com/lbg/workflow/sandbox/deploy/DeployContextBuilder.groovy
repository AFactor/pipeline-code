package com.lbg.workflow.sandbox.deploy

import groovy.json.JsonSlurperClassic

class DeployContextBuilder implements Serializable {

    def deployContext

    DeployContextBuilder(release, services, servicesTokens, platforms, platformsTokens) {
        def releaseConfig = (new HashMap(new JsonSlurperClassic().parseText(release))).asImmutable()
        def servicesConfig = (new HashMap(new JsonSlurperClassic().parseText(services))).asImmutable()
        def servicesTokensConfig = (new HashMap(new JsonSlurperClassic().parseText(servicesTokens))).asImmutable()
        def platformsConfig = (new HashMap(new JsonSlurperClassic().parseText(platforms))).asImmutable()
        def platformsTokensConfig = (new HashMap(new JsonSlurperClassic().parseText(platformsTokens))).asImmutable()

        this.deployContext = new DeployContext()
        def sc = new DeployContext(servicesConfig) // avoid jenkins serialization issues

        def servicesTokensTracker = [:]
        for (def s in servicesTokensConfig.services) {
            servicesTokensTracker[s.name] = s
        }
        def servicePlatformsTracker = [:]
        for (def ps in platformsTokensConfig.services) {
            servicePlatformsTracker[ps.name] = ps.platforms
        }
        def servicesList = []
        for (def ds in sc.services) {
            def s = ds
            s.tokens = servicesTokensTracker[ds.name].tokens
            s.platforms = servicePlatformsTracker[ds.name]
            servicesList.add(s)
        }

        deployContext.release = releaseConfig.release
        deployContext.platforms = platformsConfig.platforms
        deployContext.services = servicesList
    }
}
