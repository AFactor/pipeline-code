#!/bin/bash
set -ex

export HTTP_PROXY="http://10.113.140.187:3128"
export HTTPS_PROXY="http://10.113.140.187:3128"
export http_proxy="http://10.113.140.187:3128"
export https_proxy="http://10.113.140.187:3128"
export no_proxy=localhost,127.0.0.1,sandbox.local,lbg.eu-gb.mybluemix.net,lbg.eu-gb.bluemix.net

# expects true or false
IS_PUBLIC_DEPLOYMENT=${1:-false}

function prepareForPublicDeployment() {
    : '
    Prepare For public Deployment.

    - deletes .npmrc, .cfignore, npm-shrinkwrap.json files
    - creates .cfignore for bluemix deployment
    '

    echo 'Public deployment <- Deleting .npmrc, .cfignore, npm-shrinkwrap.json'
    rm -f .npmrc
    rm -f .cfignore
    rm -f npm-shrinkwrap.json

    echo 'Creating .cfignore'
    echo "dist/
    node_modules/*
    !node_modules/cma-common-modules
    !node_modules/babel-preset-lloyds
    !node_modules/logger
    !node_modules/loopback-connector-cloudant
    tests/
    gulp/
    *.iml
    .targets
    .apiconnect
    coverage" >> .cfignore
}

function deployApp() {
    echo 'deploying node app'
    cd $deployable
    cat pipelines/conf/manifest.yml
    ls -lah

    if [ "$IS_PUBLIC_DEPLOYMENT" = true ]
    then
        prepareForPublicDeployment
    fi;

    ls -lah
    cf logout
    cf login -a $BM_API -u $BM_USER -p $BM_PASS -o $BM_ORG -s $BM_ENV
    cf delete ${APP} -f -r || echo "Failed to delete application."
    cf push -f pipelines/conf/manifest.yml
}
