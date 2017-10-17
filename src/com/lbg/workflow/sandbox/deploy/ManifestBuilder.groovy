package com.lbg.workflow.sandbox.deploy

class ManifestBuilder implements Serializable {

	String build(String appName, Service service, DeployContext deployContext) {
		def bluemix = [:]
		bluemix.putAll(deployContext.bluemix)
		if (null != service.bluemix) {
			for (e in service.bluemix) {
				bluemix[e.key] = e.value
			}
		}
		def manifest
		if (service.buildpack == "Liberty") {
			manifest = libertyManifest(appName, service.env['LIBERTY_SERVER'], bluemix)
		}
		else if (service.buildpack == "Staticfile") {
			manifest = staticfileManifest(appName, bluemix)
		}
		else {
			manifest = defaultManifest(appName, bluemix)
		}
		if (null != service.env) {
			for (e in service.env) {
				if (!(manifest ==~ /(?s).*\s+${e.key}:.*/)) {
					manifest += "\n            ${e.key}: \"${e.value}\""
				}
			}
		}
		return manifest
	}

	String build(String appName, HashMap env, HashMap bluemix) {
		String manifest = defaultManifest(appName, bluemix)
		if (env != null) {
			for (e in env) {
				if (!(manifest ==~ /(?s).*\s+${e.key}:.*/)) {
					manifest += "\n            ${e.key}: \"${e.value}\""
				}
			}
		}
		return manifest
	}

	String buildEnvs(String manifest, HashMap envs) {
		if (envs != null) {
			for (e in envs) {
				if (!(manifest ==~ /(?s).*\s+${e.key}:.*/)) {
					manifest += "\n            ${e.key}: \"${e.value}\""
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
          services: [${bluemix.services ?: ''}]
          env:
            NODE_MODULES_CACHE: false """
	}

	private String libertyManifest(appName, server, bluemix) {
		return """
        applications:
        - name: ${appName}
          command: export IBM_JAVA_OPTIONS="\$IBM_JAVA_OPTIONS -Dhttp-port=\$PORT"; wlp/bin/server run ${server}
          buildpack: liberty-for-java
          org: ${bluemix.org}
          space: ${bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          services: [${bluemix.services ?: ''}]
          env:
            JAVA_HOME: /home/vcap/app/.java/jre """
	}

	private String staticfileManifest(appName, bluemix) {
		return """
        applications:
        - name: ${appName}
          org: ${bluemix.org}
          space: ${bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
          stack: cflinuxfs2
          services: [${bluemix.services ?: ''}]
          env:
            NODE_MODULES_CACHE: false """
	}
}
