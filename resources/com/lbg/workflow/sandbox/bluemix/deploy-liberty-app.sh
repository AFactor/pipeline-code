#!/bin/bash
set -ex

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