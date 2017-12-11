#!/bin/bash
set -ex

export HTTP_PROXY="http://10.113.140.187:3128"
export HTTPS_PROXY="http://10.113.140.187:3128"
export http_proxy="http://10.113.140.187:3128"
export https_proxy="http://10.113.140.187:3128"
export no_proxy=localhost,127.0.0.1,sandbox.local,lbg.eu-gb.mybluemix.net,lbg.eu-gb.bluemix.net

function deployApp() {
    echo 'deploying node app'
    cd $deployable
    cat pipelines/conf/manifest.yml
    cf logout
    cf login -a $BM_API -u $BM_USER -p $BM_PASS -o $BM_ORG -s $BM_ENV
    cf delete ${APP} -f -r || echo "Failed to delete application."
    cf push -f pipelines/conf/manifest.yml
}