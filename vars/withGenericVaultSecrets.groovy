/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */
import com.lbg.workflow.sandbox.ServiceDiscovery

def call(Map<String,String> mapping, Closure body) {

	if (! env.VAULT_TOKEN) {
		error "withGenericVaultSecrets: Cannot inject vault secrets without VAULT_TOKEN."
	}

	def locator = new ServiceDiscovery()
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
		vaultUrl: locator.locate('vault')
	]) { body() }
}