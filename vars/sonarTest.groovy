/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()


	node() {
		checkout scm

		def sonarServer = config.sonarServer
		def sonarProject = config.sonarProject
		def targetBranch = config.targetBranch
		def qualityGate = config.qualityGate
		def exclusions = config.exclusions
		def coverageExclusions = config.coverageExclusions
		def coverageReportFile = config.coverageReportFile
		def coverageStash = config.coverageStash
		//PCA-CWA-QP

		withSonarQubeEnv("${sonarServer}") {
			sh " rm -f ${coverageReportFile} "
			unstash	coverageStash
			sh "ls"

			sh """ source pipelines/scripts/functions && \\
						sonar-runner -X \\
							-Dsonar.host.url=${SONAR_HOST_URL} \\
							-Dsonar.jdbc.url=\'${SONAR_JDBC_URL}\' \\
							-Dsonar.jdbc.username=${SONAR_JDBC_USERNAME} \\
							-Dsonar.jdbc.password=\'${SONAR_JDBC_PASSWORD}\' \\
							-Dsonar.login=${SONAR_LOGIN} \\
							-Dsonar.pasword=${SONAR_PASSWORD} \\
							-Dsonar.projectKey=${sonarProject} \\
							-Dsonar.projectName=${sonarProject} \\
							-Dappname=${sonarProject} \\
							-DbranchName=${targetBranch} \\
							-Dsonar.projectVersion=${env.BUILD_NUMBER} \\
							-Dsonar.sources=. \\
							-Dsonar.exclusions=\'${exclusions}\' \\
							-Dsonar.coverage.exclusions=\'${coverageExclusions}\' \\
				-Dsonar.javascript.lcov.reportPath=${coverageReportFile} \\
				-Dsonar.sourceEncoding=UTF-8 \\
				-Dsonar.qualitygate=${qualityGate}\\
				-Dsonar.scm.enabled=true

		"""
		}

	}
}
String name(){
	return "Sonar"
}
return this;