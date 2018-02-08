import com.lbg.workflow.sandbox.ServiceDiscovery

def call(String appRole, Map<String,String> mapping, Closure body) {

    if (!appRole) {
        error "withVaultAppRole.groovy: Cannot inject vault secrets without VAULT_APP_ROLE."
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

    def configuration = [$class: 'VaultConfiguration',
                         vaultUrl: locator.locate('vault'),
                         vaultCredentialId: appRole]

    wrap([$class: 'VaultBuildWrapper', configuration: configuration, vaultSecrets: secretList]) {
        body()
    }
}
