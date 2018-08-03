export HTTP_PROXY=10.113.140.187:3128
export HTTPS_PROXY=10.113.140.187:3128
export http_proxy=10.113.140.187:3128
export https_proxy=10.113.140.187:3128
export no_proxy=localhost,127.0.0.1,sandbox.local,lbg.eu-gb.mybluemix.net,lbg.eu-gb.bluemix.net,10.113.140.170,10.113.140.179,10.113.140.187,10.113.140.168,jenkins.sandbox.extranet.group,nexus.sandbox.extranet.group,gerrit.sandbox.extranet.group,sonar.sandbox.extranet.group


echo APP_ID is $APP_ID
echo API_USERNAME is $API_USERNAME

# Curl log level
# LOG_LEVEL="-v"
LOG_LEVEL="--silent"

RESULTS_DIR=${WORKSPACE}/veracodeResults
TODAY=`date "+%Y%m%d"`

DETAILED_REPORT_PDF_FILE="$RESULTS_DIR"/"veracode.detailed.$TODAY.pdf"
DETAILED_REPORT_XML_FILE="$RESULTS_DIR"/"veracode.detailed.$TODAY.xml"
SUMMARY_REPORT_PDF_FILE="$RESULTS_DIR"/"veracode.summary.$TODAY.pdf"

PRESCAN_SLEEP_TIME=300
SCAN_SLEEP_TIME=300

function check_xml {
	local response="$1"

	# Check if response is XML
	if ! [[ "$response" =~ (\<\?xml version=\"1\.0\" encoding=\"UTF-8\"\?\>) ]]; then
		echo "[-] Response body is not XML format at `date`"
		echo "$response"
		exit 1
	fi
}

function check_error {
	local response="$1"

	# Check for an error element
	if [[ "$response" =~ (\<error\>.+\</error\>) ]]; then
		local error=$(echo $response | sed -n 's/.*<error>\(.*\)<\/error>.*/\1/p')
		echo "[-] Error: $error"
		exit 1
	fi
}

# Validate HTTP response
function validate_response {
	check_xml "$1"
	check_error "$1"
}

# To verify that downloaded report is really PDF and not some other format or error
function validate_pdf {
	if ! $(file $1 | grep -q "PDF document") ; then
		echo "$1 is not a PDF document: `file $1`"
		exit 1
	fi
}

# Checks the state of the application prior to attempting to create a new build and download the previous build if available
function check_state {
	echo "[+] Checking application build state"
	local build_info_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/getbuildinfo.do -F "app_id=$APP_ID"`
	validate_response "$build_info_response"

	if  [[ "$build_info_response" =~ (status=\"Results Ready\" engine_version=\"[0-9]+\"/\>) ]]; then
		VC_BUILD_ID=$(echo $build_info_response | sed -n 's/.* build_id=\"\([0-9]*\)\" .*/\1/p')

		echo "[+] Previous scan build id -  $VC_BUILD_ID"
		export VC_BUILD_ID
	else
		echo "[+] Scan results not ready:"
		echo "$build_info_response"
		exit 1
	fi
}

# Check if the state allows new builds and resets it if not
function reset_state {
	echo "[+] Checking application build state"
	local build_info_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/getbuildinfo.do -F "app_id=$APP_ID"`
	check_xml "$build_info_response"

	# TODO: Figure out why was there custom logic for Incomplete state only
	#       as in this function we want to reset state regardless of current one
	# if  [[ "$build_info_response" =~ (status=\"Incomplete\") ]]; then
		local reset_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/4.0/deletebuild.do --data "app_id=$APP_ID"`

		echo "[+] Reset app build state:"
		echo "${reset_response}"
	# fi
}

# Download reports
function download {
	if ! [[ -d "$RESULTS_DIR" ]]; then
		mkdir -p "$RESULTS_DIR"
	fi

	echo "[+] Downloading detailed report PDF"
	curl --compressed -o "$DETAILED_REPORT_PDF_FILE" -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/4.0/detailedreportpdf.do?build_id=$VC_BUILD_ID
	validate_pdf "$DETAILED_REPORT_PDF_FILE"

	echo "[+] Downloading detailed report XML"
	curl --compressed -o "$DETAILED_REPORT_XML_FILE" -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/4.0/detailedreport.do?build_id=$VC_BUILD_ID

	echo "[+] Downloading summary report PDF"
	curl --compressed -o "$SUMMARY_REPORT_PDF_FILE" -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/4.0/summaryreportpdf.do?build_id=$VC_BUILD_ID
	# Don't validate pdf here, as even if this fails we still want to send out mail with already validated detailed report

	# Validate files were downloaded
	if ! [[ -f $DETAILED_REPORT_PDF_FILE ]]; then
		echo "[-] Detailed PDF report failed to download"
		exit 1
	fi

	if ! [[ -f $DETAILED_REPORT_XML_FILE ]]; then
		echo "[-] Detailed XML report failed to download"
		exit 1
	fi

	if ! [[ -f $SUMMARY_REPORT_PDF_FILE ]]; then
		echo "[-] Summary PDF report failed to download"
		exit 1
	fi
}

# Create new build
function createbuild {
	echo "[+] Creating a new Veracode build named \"$VERSION\" for application #$APP_ID"

	local create_build_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/createbuild.do -F "app_id=$APP_ID" -F "version=$VERSION"`
	validate_response "$create_build_response"

	# Extract build id
	VC_BUILD_ID=$(echo $create_build_response | sed -n 's/.* build_id=\"\([0-9]*\)\" .*/\1/p')
}

# Upload files
function uploadfiles {
	for file in $(find $UPLOAD_DIR)
	do
		if [[ -f "$file" ]]; then
			echo "[+] Uploading $file"
			local upload_file_response=`curl --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/uploadfile.do -F "app_id=$APP_ID" -F "file=@$file"`
			validate_response "$upload_file_response"
		fi
	done

	# Validate all files were successfully uploaded
	for file in $(find $UPLOAD_DIR)
	do
		if [[ -f "$file" ]]; then
			if ! [[ "$upload_file_response" =~ (\<file file_id=\"[0-9]+\" file_name=\""${file##*/}"\" file_status=\"Uploaded\"/\>) ]]; then
				echo "[-] Error uploading $file"
				exit 1
			fi
		fi
	done
}

# Begin pre-scan
function beginprescan {
	echo "[+] Starting pre-scan of uploaded files"
	local pre_scan_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/beginprescan.do -F "app_id=$APP_ID" -F "auto_scan=true"`
	validate_response "$pre_scan_response"
}

# Poll pre-scan status
function pollprescan {
	echo "[+] Polling pre-scan status every $PRESCAN_SLEEP_TIME seconds"
	local is_pre_scanning=true
	while $is_pre_scanning; do
		sleep $PRESCAN_SLEEP_TIME

		local build_info_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/getbuildinfo.do -F "app_id=$APP_ID"`
		validate_response "$build_info_response"

		# Check if pre-scan is successful
		if [[ "$build_info_response" =~ (\<analysis_unit analysis_type=\"Static\" status=\"Pre-Scan Success\" engine_version=\"[0-9]+\"/\>) ]]; then
			is_pre_scanning=false
			echo -e "\n[+] Pre-scan complete"
		else
			echo -n -e "."
		fi
	done
}

# Begin application scan
function beginscan {
	echo "[+] Starting scan of uploaded files"
	local begin_scan_response=`curl ${LOG_LEVEL} --compressed -u "$API_USERNAME:$API_PASSWORD" https://analysiscenter.veracode.com/api/5.0/beginscan.do  -F "app_id=$APP_ID" -F "scan_all_top_level_modules=true"`
	validate_response "$begin_scan_response"
}
