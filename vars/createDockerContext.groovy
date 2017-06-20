/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

import com.cloudbees.groovy.cps.NonCPS


def call(String name){

	def dockerFiles = [:]
	def composeFile = ''
	def dockerIgnore = ''
	switch (name) {
		case 'node48':
			dockerFiles['Dockerfile.node.allmodules'] = 'com/lbg/workflow/sandbox/docker/Dockerfile.node.allmodules.48'
			dockerFiles['Dockerfile.node.base'] = 'com/lbg/workflow/sandbox/docker/Dockerfile.node.base.48'
			composeFile = 'com/lbg/workflow/sandbox/docker/docker-compose.node.yml'
			dockerIgnore = 'com/lbg/workflow/sandbox/docker/dockerignore.node'
			break
		case 'node610':
			dockerFiles['Dockerfile.node610.allmodules'] = 'com/lbg/workflow/sandbox/docker/Dockerfile.node.allmodules.610'
			dockerFiles['Dockerfile.node610.base'] = 'com/lbg/workflow/sandbox/docker/Dockerfile.node.base.610'
			composeFile = 'com/lbg/workflow/sandbox/docker/docker-compose.node610.yml'
			dockerIgnore = 'com/lbg/workflow/sandbox/docker/dockerignore.node'
			break
		default :
			error "createDockerContext: Invalid argument  ${name}"
			break
	}



	for (entry in entries(dockerFiles)){
		def dockerfileContent = libraryResource entry[1]
		writeFile file: entry[0] , text: dockerfileContent
	}
	def composeContent = libraryResource composeFile
	writeFile file: 'docker-compose.yml' , text: composeContent

	def dockerIgnoreContent = libraryResource dockerIgnore
	writeFile file: '.dockerignore' , text: dockerIgnoreContent
}


@NonCPS def entries(m) {m.collect {k, v -> [k, v]}}
