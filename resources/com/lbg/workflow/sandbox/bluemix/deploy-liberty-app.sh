#!/bin/bash
set -ex

export HTTP_PROXY="http://10.113.140.187:3128"
export HTTPS_PROXY="http://10.113.140.187:3128"
export http_proxy="http://10.113.140.187:3128"
export https_proxy="http://10.113.140.187:3128"
export no_proxy=localhost,127.0.0.1,sandbox.local,lbg.eu-gb.mybluemix.net,lbg.eu-gb.bluemix.net,10.113.140.170,10.113.140.179,10.113.140.187,10.113.140.168,jenkins.sandbox.extranet.group,nexus.sandbox.extranet.group,gerrit.sandbox.extranet.group,sonar.sandbox.extranet.group,extranet.group

function deployApp() {
    echo 'deploying liberty app'
    cd $deployable
    cat pipelines/conf/manifest.yml
    cf logout
    cf login -a $BM_API -u $BM_USER -p $BM_PASS -o $BM_ORG -s $BM_ENV
    cf delete ${APP} -f -r || echo "Failed to delete application."
    cf push ${APP} -f pipelines/conf/manifest.yml -p ${ZIPFILE} -t 180
    sleep 60
}