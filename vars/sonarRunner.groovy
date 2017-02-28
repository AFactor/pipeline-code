/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

def call(Closure body) {

	def config = [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def sonarServer = config.sonarServer
	def headers = config.preRun ?: '~/.bashrc'
	def javaOptions = config.javaOptions?: [:]

	def javaOptionString = ' '

	for (def entry: javaOptions){
		javaOptionString = javaOptionString + "${entry.key}=${entry.value} "
	}
	if (sonarServer == null) {
		error "Need a SonarServer ID to run against. Failed"
	}
	withSonarQubeEnv("${sonarServer}") { 
		sh """ source ${headers} && \\
						sonar-runner -X \\
							-Dsonar.host.url=${SONAR_HOST_URL} \\
							-Dsonar.jdbc.url=\'${SONAR_JDBC_URL}\' \\
							-Dsonar.jdbc.username=${SONAR_JDBC_USERNAME} \\
							-Dsonar.jdbc.password=\'${SONAR_JDBC_PASSWORD}\' \\
							-Dsonar.login=${SONAR_LOGIN} \\
							-Dsonar.pasword=${SONAR_PASSWORD} \\
							-Dsonar.sourceEncoding=UTF-8 \\
							${javaOptionString}
							""" }
}
String name(){
	return "SonarQube Runner"
}
return this;