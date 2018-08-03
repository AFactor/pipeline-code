#!/bin/bash
source ${WORKSPACE}/pipelines/scripts/veracode_functions.sh
# set -x

echo "Init - `date`"
check_state
download
echo "Complete - `date`"
