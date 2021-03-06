import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

/*
 * Runs nexusIQ scan on the provided artifact. All config is read from main
 * job config context specified usually in pipelines/conf/job-config.json
 * Defaults are provided to cover most common settings.
 * Package to scan is usually prepared in package stage of the pipeline, but
 * for scan purposes, you only need to provide stash name and scan pattern
 * which should match the artifact name.
 *
 * branchName parameter determines branch name to scan. By default, only master
 * branches will be scanned, but this can be overridden in config by any regex.
 */

def runTest(String targetBranch, context){
	def cfg = getConfig(targetBranch, context)
	def fail = false

	if (cfg.runScan){
		// lock so that we can retrieve report for current branch before next scan begins
		lock(quantity: 1, resource: cfg.iqApp){
			createApp(cfg)
			fail = runScan(cfg)
			getReport(cfg)

			// always get detailed report first before potentially breaking the build
			if (fail){
				error("nexusIQ: One of build breaker thresholds reached. See scan logs for details.")
			}
		}
	} else {
		print "Scan not enabled for ${targetBranch}, skipping nexusIQ analysis..."
	}
}

def publishSplunk(targetBranch, epoch, context, handler){
	def cfg = getConfig(targetBranch, context)

	if (cfg.runScan){
		unstash "nexusIqResult"
		handler.SCP(cfg.result, "${cfg.splunkDir}/${epoch}_${cfg.result}")
	} else {
		print "No nexusIQ scan was run, nothing to publish."
	}
}

String name(){
	return "nexusIQ"
}

// Prepare configuration for nexusIQ api and scan. Use defaults where value is not specified.
def getConfig(String targetBranch, context){
	def nxIQctx=context.config.nexusIQ
	def cfg=[:]

	cfg.iqApp         = context.application
	cfg.report        = nxIQctx?.reportFile?:  "${cfg.iqApp}-IQreport.json"
	cfg.result        = nxIQctx?.resultFile?:  "${cfg.iqApp}-IQresult.json"
	cfg.iqStage       = nxIQctx?.stage?:       'build'
	cfg.credsId       = nxIQctx?.credentials?: 'NexusIQ-SRVAPPOSSJNKOB01'
	cfg.scanPattern   = nxIQctx?.scanPattern?: '*'
	cfg.artifactStash = nxIQctx?.stash?:       'artifactStash'
	cfg.nodeLabel     = nxIQctx?.nodeLabel?:   'nexusIQ'
	cfg.apiNode       = nxIQctx?.apiLabel?:    'lbg_slave'
	cfg.splunkDir     = nxIQctx?.reportdir?:   "/apps/splunkreports/${cfg.iqApp}"

	// Generous default build breakers before we adopt any standards
	cfg.affected      = nxIQctx?.affected?:    100000
	cfg.critical      = nxIQctx?.critical?:    100000
	cfg.severe        = nxIQctx?.severe?:      100000
	cfg.moderate      = nxIQctx?.moderate?:    100000

	cfg.nexusIqApi    = nxIQctx?.api?:   "https://nexusiqapp.service.group:8070/api/v2"
	cfg.orgID         = nxIQctx?.orgID?: "d101c545f626458f849403cfed83d709"  // ENGR Open Banking
	cfg.contact       = nxIQctx?.lbgID?: "9209598"
	// LBG id of an existing user. Doesn't matter who it is, actual owner will be set to certificate user

	cfg.branchType	  = nxIQctx?.branchType?: ['integration', 'patchset', 'PR', 'feature']
	cfg.runScan 	  = context.branchType in cfg.branchType

	// If branch is specified with regex, it overrides branchType config
	if (nxIQctx?.branch?.trim()){
		if (targetBranch =~ nxIQctx.branch){
			cfg.runScan = true
		} else {
			cfg.runScan = false
		}
	}

	print "Loaded nexusIQ config..."
	return cfg
}

def createApp(cfg){
	def newApp="""{
		"name": "${cfg.iqApp}",
		"publicId": "${cfg.iqApp}",
		"organizationId": "${cfg.orgID}",
		"contactUserName": "${cfg.contact}"
	}"""

	node(cfg.apiNode){
		print "Ensuring ${cfg.iqApp} is present in nexusIQ server..."
		resp = iqCurl("-v -data '${newApp}' '${cfg.nexusIqApi}/applications' 2>&1 || true", cfg)

		if (! resp.contains("${cfg.iqApp} is already used as a name.") &&
			! resp.contains("HTTP Response: 200")){
				errormsg="Failed creating ${cfg.iqApp} in nexusIQ! Aborting."
				print errormsg
				print resp
				error(errormsg)
			}
	} //node
}

def runScan(cfg){
	def failed = false
	node(cfg.nodeLabel){
		unstash cfg.artifactStash
		print "About to initiate nexusIQ scan..."

		try {
			results = nexusPolicyEvaluation failBuildOnNetworkError: true,
				iqApplication: cfg.iqApp,
				iqScanPatterns: [[scanPattern: cfg.scanPattern]],
				iqStage: cfg.iqStage,
				jobCredentialsId: cfg.credsId

			// evaluate build breakers
			if (results.affectedComponentCount > cfg.affected) { failed = true
				print "${results.affectedComponentCount} affected is more than limit of ${cfg.affected}" }

			if (results.criticalComponentCount > cfg.critical) { failed = true
				print "${results.criticalComponentCount} critical is more than limit of ${cfg.critical}" }

			if (results.severeComponentCount > cfg.severe)     { failed = true
				print   "${results.severeComponentCount} severe is more than limit of ${cfg.severe}" }

			if (results.moderateComponentCount > cfg.moderate) { failed = true
				print "${results.moderateComponentCount} moderate is more than limit of ${cfg.moderate}" }

			if (failed){
				print "One of build breaker thresholds reached. Failing build after obtaining detailed scan report."
			}

			// Show real report url which is on nexusiquser.intranet.group
			def reportUrl = results.applicationCompositionReportUrl.replaceFirst(
				"nexusiqapp.service.group", "nexusiquser.intranet.group")
			print "Fixed report url: ${reportUrl}"

			// Prepare evaluation results json
			def evalResults = JsonOutput.toJson(
				affected: results.affectedComponentCount,
				critical: results.criticalComponentCount,
				severe:   results.severeComponentCount,
				moderate: results.moderateComponentCount,
				report:   reportUrl
			)
			writeFile file: cfg.result, text: evalResults

			stash name: "nexusIqResult", includes: cfg.result
			archiveArtifacts cfg.result
			step([$class: 'WsCleanup', notFailBuild: true])
		} catch (e){
			print "NexusIQ scan failed: "
			print e
			throw e
		}
	}
	return failed
}

def getReport(cfg){
	def appID
	def repUrl

	node(cfg.apiNode){
		print "Retrieving scan report..."

		try {
			appJson = iqCurl("${cfg.nexusIqApi}/applications?publicId=${cfg.iqApp}", cfg)
			appID = new JsonSlurperClassic().parseText(appJson).applications[0].id
		} catch (e){
			print "Got error while getting app ID:"
			print e.message
			throw e
		}

		try {
			reportsJson = iqCurl("${cfg.nexusIqApi}/reports/applications/${appID}", cfg)
			repUrl = new JsonSlurperClassic().parseText(reportsJson)[0].reportDataUrl
		} catch (e){
			print "Got error while getting report URLs:"
			print e.message
			throw e
		}

		try {
			// remove api/v2 from reports url as that is already part of cfg.nexusIqApi
			reportUrl = repUrl.replaceFirst("api/v2","")
			getReport = iqCurl("${cfg.nexusIqApi}/${reportUrl} | python -mjson.tool >${cfg.report}", cfg)
		} catch (e){
			print "Got error while getting report:"
			print e.message
			throw e
		}

		archiveArtifacts cfg.report
		step([$class: 'WsCleanup', notFailBuild: true])
	}
}

def iqCurl(String request, cfg){
	def response

	// Certificate for accessing nexusiqapi server
	withCredentials([certificate(aliasVariable: 'alias', credentialsId: cfg.credsId,
								 keystoreVariable: 'key', passwordVariable: 'pass')]){
		def password = "${env.pass}"
		def key = "${env.key}"

		// jcurl.jar file - to provide standalone tool for api. Requires java 1.7+
		withCredentials([file(credentialsId: 'jcurl', variable: 'jcurl')]){
			try {
				response = sh(returnStdout: true, script: "java -jar ${jcurl} -k \
					-keystore ${key} -storepass '${password}' \
					-b CLM-CSRF-TOKEN myToken -H X-CSRF-TOKEN myToken \
					${request}").trim()
			} catch (e){
				print "nexusIQapi - got error running ${request}:"
				print e.message
			}
		} //jcurl
	} //creds
	return response
}

return this;
