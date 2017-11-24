package com.lbg.workflow.sandbox.deploy

class ManifestBuilder implements Serializable {

	String build(String appName, service, deployContext) {
		def bluemix = [:]
		bluemix.putAll(deployContext.platforms.bluemix)
		if (null != service?.platforms?.bluemix) {
			for (e in service.platforms.bluemix) {
				bluemix[e.key] = e.value
			}
		}
		def manifest
		if (service.type == "Liberty") {
			// TODO: use './server list' to obtain liberty server
			manifest = libertyManifest(appName, service.platforms.bluemix['LIBERTY_SERVER'], bluemix)
		}
		else if (service.type == "Java") {
			manifest = javaManifest(appName, service.platforms.bluemix['JBP_CONFIG_JAVA_MAIN'], bluemix)
		}
		else if (service.type == "Staticfile") {
			manifest = staticfileManifest(appName, bluemix)
		}
		else {
			manifest = defaultManifest(appName, bluemix)
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

	String defaultManifest(appName, bluemix) {
		return """
        applications:
        - name: ${appName}
          org: ${bluemix.org}
          space: ${bluemix.space ?: bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          services: [${bluemix.services ?: ''}]
          env:
            NODE_MODULES_CACHE: false """
	}

	String libertyManifest(appName, server, bluemix) {
		return """
        applications:
        - name: ${appName}
          command: export IBM_JAVA_OPTIONS="\$IBM_JAVA_OPTIONS -Dhttp-port=\$PORT"; wlp/bin/server run ${server}
          buildpack: liberty-for-java
          org: ${bluemix.org}
          space: ${bluemix.space ?: bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          services: [${bluemix.services ?: ''}]
          env:
            JAVA_HOME: /home/vcap/app/.java/jre """
	}

	String staticfileManifest(appName, bluemix) {
		return """
        applications:
        - name: ${appName}
          org: ${bluemix.org}
          space: ${bluemix.space ?: bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
          stack: cflinuxfs2
          services: [${bluemix.services ?: ''}]
          env:
            NODE_MODULES_CACHE: false """
	}

	String javaManifest(appName, javaOptions, bluemix) {
		return """
        applications:
        - name: ${appName}
          buildpack: https://github.com/cloudfoundry/java-buildpack.git
          org: ${bluemix.org}
          space: ${bluemix.space ?: bluemix.env}
          disk_quota: ${bluemix.disk}
          memory: ${bluemix.memory}
          env:
            JBP_CONFIG_JAVA_MAIN: "$javaOptions" """
	}
}
