/*
 * Publishes job statistics to splunk at the end of each job run.
 * Uses SCP method (and credentialsId) from vars/splunkPublisher.groovy -
 * this is so that we can define these methods and IDs in a single place only.
 * Replaces job status from IN_PROGRESS (we get this as we are getting
 * stats while the job is still running) to desired result.
 *
 * If there is failure retrieving stats (e.g. 404), -f parameter of curl would cause
 * it to exit with 22. This would then produce no stats file and splunk upload will
 * fail. Nevertheless this wouldn't break job run, as this error is caught, but not
 * thrown further.
 */

package com.lbg.workflow.sandbox

def toSplunk(String jobTag, String buildUrl, String credentialsId, String jobStatus, String endPoint){
    node('framework'){
        filename = "${jobTag}-stats.json"
        if (!endPoint) {
            endPoint = "/apps/splunkreports/jenkinsstats/all/"
        }
        try {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId,
                              usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]){
                sh "curl --insecure -f -s --user ${USERNAME}:${PASSWORD} ${buildUrl}/wfapi/describe | \
              python -c \'import json,sys; s=json.load(sys.stdin); s[\"status\"]=\"${jobStatus}\"; \
                          print json.dumps(s)\' > ${filename}"
            }
            splunkPublisher.SCP(filename, endPoint)
        } catch (error) {
            echo "Post build: got error during job stats publishing, continuing: ${error}"
        }
    }
}
