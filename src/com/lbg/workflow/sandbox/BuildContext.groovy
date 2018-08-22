package com.lbg.workflow.sandbox

class BuildContext implements Serializable{
	final HashMap config		// usually pipelines/config/job-config.json
	final String application	// app name as specified in invocation
	final String branchType		// integration, PR, patchset, feature
	final String branchName		// name of the branch, to avoid scm checkouts to get it

	BuildContext(String configuration) {
		this.application = 'default'
		this.config = (new HashMap(
						new groovy.json.JsonSlurperClassic().
						parseText(configuration)
					)
				).asImmutable()
	}

	BuildContext(String application, String configuration) {
		this.application = application
		this.config = (new HashMap(
						new groovy.json.JsonSlurperClassic().
						parseText(configuration)
					)
				).asImmutable()
	}

	BuildContext(String application, String configuration, String branchType, String branchName) {
		this.application = application
		this.config = (new HashMap(
						new groovy.json.JsonSlurperClassic().
						parseText(configuration)
					)
				).asImmutable()
		this.branchType = branchType
		this.branchName = branchName
	}
}
