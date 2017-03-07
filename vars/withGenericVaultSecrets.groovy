/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */
import groovy.json.internal.LazyMap

def call(Map<String,String> mapping, Closure body) {


	def secretList = []

	for( item in mapping){

		def pathTokens = item.value.split('/')
		def vaultKey = pathTokens.last()
		def path = pathTokens[0]
		for (int i = 1; i< pathTokens.size()-1; i++){
			path += '/'
			path += pathTokens[i]
		}
		def envVar = item.key

		echo "Mapping Secret:${vaultKey} from vault:${path} to ${envVar}"
		secretList.add([$class: 'VaultSecret',
			path: "${path}",
			secretValues: 	[
				[ 	$class: 'VaultSecretValue',
					envVar: "${envVar}", vaultKey: "${vaultKey}"
				]
			]
		])
	}

	wrap( [$class: 'VaultBuildWrapper',
		vaultSecrets: secretList,
		authToken: env.VAULT_TOKEN,
		vaultUrl: 'http://10.113.140.168:8200'
	]) {
		body()
	}
}