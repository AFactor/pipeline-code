/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def createParentRepo(String journey, String username, String email, Integer objectsizelimit ) {

	node('framework'){

		def curr_date=new Date().format( 'dd-MM-yyyy HH:mm' )
		def GERRIT_HTTP_CREDS
		def command = '''
		#Create parent admin group
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit create-group --owner Administrators --description \"'group for administrators'\" ${journey}-admin || echo ""
		#Create reviewers group
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit create-group --owner "${journey}-admin" --description \"'group for reviewers'\" ${journey}-reviewer || echo ""
		#Create developers group
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit create-group --owner "${journey}-admin" --description \"'group for developer'\" $journey || echo ""
		#Add user to administrators group
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit set-members -a $email ${journey}-admin
		#Create project
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit create-project $journey --permissions-only --max-object-size-limit ${objectsizelimit}m -d "'Description: Parent Repository to adhere with gerrit verified label;\n\\
Owner: username;\n\\
Owner Email ID: $email ;\n\\
Created on: $curr_date'"  || echo ""
		ssh -p 29418 -o StrictHostKeyChecking=no bluemixdeploy@gerrit.sandbox.extranet.group gerrit create-project ${journey}-noverify --permissions-only --max-object-size-limit ${objectsizelimit}m -d "'Description: Parent Repository not to adhere with gerrit verified label;\n\\
Owner: $username ;\n\\
Owner Email ID: $email ;\n\\
Created on: $curr_date '"  || echo ""
		#Changes for access
		for repo in ${journey} ${journey}-noverify
		do
			mkdir -p tmp ; cd tmp
	 		git init
			git remote add origin ssh://bluemixdeploy@gerrit.sandbox.extranet.group:29418/$repo
			git fetch origin refs/meta/config:refs/remotes/origin/meta/config
			git checkout meta/config
			#Prepare project.config
			git config -f project.config --remove-section access.inheritFrom || echo ""
			git config -f project.config access.inheritFrom "All-Projects"
			git config -f project.config --remove-section access.refs/* || echo ""
			git config -f project.config access.refs/*.owner "group ${journey}-admin"
			git config -f project.config access.refs/*.label-Code-Review "-1..+1 group ${journey}"
			git config -f project.config --add access.refs/*.label-Code-Review "-2..+2 group ${journey}-reviewer"
			git config -f project.config --remove-section project || echo ""
			if [ "$repo" == "${journey}-noverify" ];then
				git config -f project.config project.description "'Description: Parent Repository not containing gerrit verified label;\n\\
Owner: $username;\n\\
Owner Email ID: $email;\n\\
Created/modified on: $curr_date'"
			elif [ "$repo" == "${journey}" ];then
				git config -f project.config project.description "'Description: Parent Repository containing gerrit verified label;\n\\
Owner: $username;\n\\
Owner Email ID: $email;\n\\
Created/modified on: $curr_date'"
				git config -f project.config --remove-section label.Verified || echo ""
				git config -f project.config label.Verified.defaultValue "0"
				git config -f project.config label.Verified.function "MaxWithBlock"
				git config -f project.config label.Verified.value "-1 Fails"
				git config -f project.config --add label.Verified.value "0 No score"
				git config -f project.config --add label.Verified.value "+1 Verified"
				git config -f project.config access.refs/*.label-Verified "-1..+1 group Non-Interactive Users"
			fi
			git config -f project.config --remove-section access.refs/heads/* || echo ""
			git config -f project.config access.refs/heads/*.read "group ${journey}"
			git config -f project.config access.refs/heads/*.create "group ${journey}"
			git config -f project.config access.refs/heads/*.push "group ${journey}"
			git config -f project.config --add access.refs/heads/*.read "group ${journey}-reviewer"
			git config -f project.config --add access.refs/heads/*.create "group ${journey}-reviewer"
			git config -f project.config --add access.refs/heads/*.push "group ${journey}-reviewer"
			git config -f project.config --remove-section access.refs/meta/config || echo ""
			git config -f project.config access.refs/meta/config.read "group ${journey}"
			git config -f project.config --add access.refs/meta/config.read "group ${journey}-reviewer"
			git config -f project.config --remove-section receive || echo ""
			git config -f project.config receive.maxObjectSizeLimit "${objectsizelimit}m"	
			#Prepare groups		
			if ! [ -f groups ];then touch groups ;	fi
			sed -i '/UUID/d;/21f95d8f37c529dd5822a9ba062ef6fa4a15adcd/d;/bc998b3f76ce681ac0d7f0ed0ce58489d060aba2/d;/global:Anonymous-Users/d;/global:Project-Owners/d;/global:Registered-Users/d' groups
			echo -e "# UUID				Group Name\n#\n21f95d8f37c529dd5822a9ba062ef6fa4a15adcd				Non-Interactive Users\nbc998b3f76ce681ac0d7f0ed0ce58489d060aba2				Administrators\nglobal:Anonymous-Users				Anonymous Users\nglobal:Project-Owners				Project Owners\nglobal:Registered-Users				Users" >> groups
			if [ "$(grep $journey groups)" == "" ];then
				journey_id=$(curl -X GET --digest -u $GERRIT_HTTP_CREDS https://gerrit.sandbox.extranet.group/a/groups/ | sed 1d | /apps/tools/jq .\\"$journey\\"."id" | sed 's/"//g')
				journey_reviewer_id=$(curl -X GET --digest -u $GERRIT_HTTP_CREDS https://gerrit.sandbox.extranet.group/a/groups/ | sed 1d | /apps/tools/jq .\\"$journey-reviewer\\"."id" | sed 's/"//g')
				journey_admin_id=$(curl -X GET --digest -u $GERRIT_HTTP_CREDS https://gerrit.sandbox.extranet.group/a/groups/ | sed 1d | /apps/tools/jq .\\"$journey-admin\\"."id" |sed 's/"//g')
				echo -e "$journey_id				$journey" >> groups
				echo -e "$journey_reviewer_id				$journey-reviewer" >> groups
				echo -e "$journey_admin_id				$journey-admin" >> groups
			fi
			git config --global user.name "bluemixdeploy" || echo ""
			git config --global user.email "bluemixdeploy@sandbox.local" || echo ""
			git add *
			git commit -am "Changes for refs modification"
			git push origin meta/config:meta/config
			cd ../ ; rm -rf tmp
		done
			'''
		withCredentials([
			usernameColonPassword(credentialsId: 'GERRIT_HTTP_CREDS', variable: 'GERRIT_HTTP_CREDS')
		]) {

			withEnv([
				"journey=$journey",
				"username=$username",
				"email=$email",
				"objectsizelimit=$objectsizelimit",
				"curr_date=$curr_date"
			]) {
				try{
					sshagent(['gerrit-admin']) { sh command }
				}catch(error){
					echo error.message 
					throw error
				}finally{
					step([$class: 'WsCleanup', notFailBuild: true])
				}
			}
		}
	}
}


def setStatus (String changeID, String revision, String message, String codereview, String verified) {

	node('framework'){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--code-review=${codereview}  \\
				--label verified=${verified}
				"""

		try{
			sshagent(['gerrit-updater']) {
				sh command
			}
		} catch(error) {
			echo error.message 
			throw error
		} finally{
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}
def sendMessage (String changeID, String revision, String message) {

	node('framework'){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message} \"' 
				"""
		try{
			sshagent(['gerrit-updater']) {
				sh command
			}
		} catch(error) {
			echo error.message 
			throw error
		} finally{
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}

def setCodeReview (String changeID, String revision, String message, String codereview) {

	node('framework'){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--code-review=${codereview}
				"""
		try{
			sshagent(['gerrit-updater']) {
				sh command
			}
		} catch(error) {
			echo error.message 
			throw error
		} finally{
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}
}
def setVerified (String changeID, String revision, String message, String verified) {

	node('framework'){
		def command = """
			ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
				gerrit review ${changeID},${revision} \\
				-m '\"${message}: ${BUILD_URL}\"' \\
				--label verified=${verified}
				"""
		try{
			sshagent(['gerrit-updater']) {
				sh command
			}
		} catch(error) {
			echo error.message 
			throw error
		} finally{
			step([$class: 'WsCleanup', notFailBuild: true])
		}
		sshagent(['gerrit-updater']) {
			sh command
		}
	}
}

def buildStarted(String changeID, String revision){
	setStatus (changeID, revision, 'Build Started', '0', '0')
}

def failCodeReview (String changeID, String revision ) {
	setCodeReview (changeID, revision, "Code Quality Failed", '-2')
}
def failTests (String changeID, String revision) {
	setVerified (changeID, revision, "Tests Failed", '-1')
}
def passCodeReview (String changeID, String revision ) {
	setCodeReview (changeID, revision, "Code Quality Passed", '+1')
}
def passTests (String changeID, String revision) {
	setVerified (changeID, revision, "Tests Passed", '+1')
}



String findTargetBranch(String targetCommit) {


	def targetBranch = ''
	def command = """
		ssh -p 29418 -o StrictHostKeyChecking=no jenkins@gerrit.sandbox.extranet.group \\
			gerrit query --format=text \\
				${targetCommit} \\
		| awk '/^([ ]+branch:)(.*)/{print \$2}'
       """
	node('framework'){
		try{
			sshagent(['gerrit-updater']) {
				targetBranch = sh(returnStdout: true, script: command).trim()
			}
		} catch(error) {
			echo error.message
			throw error
		} finally{
			step([$class: 'WsCleanup', notFailBuild: true])
		}
	}

	switch (targetBranch) {
		/*
		 * This started with the idea of returning well known identifiers
		 * example release-prod for release-prod-* so on and so forth.
		 * But there is no longer any need for that. Leave this block in
		 * place should that requirement return
		 */
		case "master": return "master" ; break
		case "release-prod": return "release-prod" ; break
		case "hotfixes": return "hotfixes" ; break
		default: return targetBranch ; break
	}
}

return this;