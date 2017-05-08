package com.lbg.workflow.sandbox.deploy

class ManifestBuilder implements Serializable {

    String build(String appName, Service service, DeployContext deployContext) {
        def bluemix = deployContext.bluemix
        if (service.bluemix) {
            for (e in service.bluemix) {
                bluemix[e.key] = e.value
            }
        }
        String manifest = defaultManifest(appName, bluemix)
        if (service.env != null) {
            for (e in service.env) {
                if (!manifest.contains(e.key)) {
                    manifest += "\n            ${e.key}: ${e.value}"
                }
            }
        }
        return manifest
    }

    String build(String appName, HashMap env, HashMap bluemix) {
        String manifest = defaultManifest(appName, bluemix)
        if (env != null) {
            for (e in env) {
                if (!manifest.contains(e.key)) {
                    manifest += "\n            ${e.key}: ${e.value}"
                }
            }
        }
        return manifest
    }

    private String defaultManifest(appName, bluemix) {
        return """
        applications:
        - name: ${appName}
          org: ${bluemix.org}
          space: ${bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          env:
            NODE_MODULES_CACHE: false """
    }
}
