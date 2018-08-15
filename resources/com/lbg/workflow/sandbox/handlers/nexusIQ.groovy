/*
 * Runs nexusIQ scan on the provided artifact. All config is read from main
 * job config context specified usually in pipelines/conf/job-config.json
 * Defaults are provided to cover most common settings. Only compulsory
 * parameter is nexusIQ application name/ID, which is unique for each app.
 * Package to scan is usually prepared in package stage of the pipeline, but
 * for scan purposes, you only need to provide stash name and scan pattern
 * which should match the artifact name.
 *
 * branchName parameter determines branch name to scan. By default, only master
 * branches will be scanned, but this can be overridden in config by any regex.
*/

def runTest(String targetBranch, context){
	String iqStage       = context.config.nexusIQ.stage?:       'build'
	String branchName    = context.config.nexusIQ.branch?:      'master'
	String credsId       = context.config.nexusIQ.credentials?: 'NexusIQ-SRVAPPOSSJNKOB01'
	String scanPattern   = context.config.nexusIQ.scanPattern?: '*'
	String artifactStash = context.config.nexusIQ.stash?:       'artifactStash'
	String nodeLabel     = context.config.nexusIQ.nodeLabel?:   'nexusIQ'
	String iqApp         = context.config.nexusIQ.iqApp?:       ''

	if (iqApp == ''){
		message = "context.config.nexusIQ.iqApp not specified. Aborting scan."
		print message
		error(message)
	}

	print "Loaded nexusIQ config..."
	if (targetBranch =~ branchName){
		node(nodeLabel){
			unstash artifactStash
			print "About to initiate nexusIQ scan..."

			try {
				nexusPolicyEvaluation failBuildOnNetworkError: true,
					iqApplication: iqApp,
					iqScanPatterns: [[scanPattern: scanPattern]],
					iqStage: iqStage,
					jobCredentialsId: credsId
			} catch (e){
				print "NexusIQ scan failed: "
				print e
				throw e
			}

			//TODO: Fetch report from nexusIQ server, get it ready for splunk
		}
	} else {
		echo "Not a ${branchName} branch, skipping nexusIQ analysis..."
	}
}

// TODO: Fetch report and have it ready to publish to splunk
def publishSplunk(String targetBranch, String epoch, context, handler){}

String name() {
	return "nexusIQ"
}

return this;
