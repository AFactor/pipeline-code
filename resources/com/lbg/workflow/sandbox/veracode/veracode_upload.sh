#!/bin/bash
source ${WORKSPACE}/pipelines/scripts/veracode_functions.sh
set -x

# Directory argument
if [[ "$1" != "" ]]; then
	UPLOAD_DIR="$1"
else
	echo "[-] Directory not specified."
	exit 1
fi

# Check if directory exists
if ! [[ -d "$UPLOAD_DIR" ]]; then
	echo "[-] Directory does not exist"
	exit 1
fi

# Version argument
if [[ "$2" != "" ]]; then
	VERSION="$2"
else
	VERSION=`date "+%Y-%m-%d %T"`	# Use date as default
fi

echo "Init - `date`"

reset_state
createbuild
uploadfiles
beginprescan

echo "Complete - `date`"
