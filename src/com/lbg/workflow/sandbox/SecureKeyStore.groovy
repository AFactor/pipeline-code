/*
 * Author: Kshitiz
 */

package com.lbg.workflow.sandbox

import com.lbg.workflow.sandbox.ServiceDiscovery
import org.apache.tools.ant.types.resources.selectors.None

/**
 * SecureKeyStore, handles different key-vaule secret stores
 */
class SecureKeyStore {

    /** String  */
    private def service

    /** Map */
    private def tokensMap

    /** ServiceDiscovery */
    private def serviceDiscovery

    /** Node Context */
    private def step

    /**
     * @param tokensMap
     * @param serviceName
     * @param step: context of the pipeline
     */
    SecureKeyStore(tokensMap, serviceName, step) {
        this.tokensMap = tokensMap
        this.service = serviceName
        this.step = step
        this.serviceDiscovery = new ServiceDiscovery()
    }

    /**
     * @param config
     *
     * @return Map
     */
    def fillWithCredentials(serviceConfig) {
        switch (this.service) {
            case 'vault':
                return this.getSecretFromVault(serviceConfig['appRole'])
                break
            /** more services can be added later */
            default:
                return this.tokensMap
                break
        }
    }

    /**
     * @param appRole
     *
     * @return Map
     */
    private def getSecretFromVault(String appRole) {
        def vaultCredentials
        this.tokensMap.each { envVar, credentials ->

            /** Ignores which is not a list, which means it is not for vault.*/
            if (credentials instanceof List) {
                def vaultPath = credentials[1]
                def vaultKey = credentials[0]

                try {
                    vaultCredentials = callVault(
                            "${vaultPath}",
                            "${vaultKey}",
                            "${envVar}",
                            "${appRole}"
                    )
                    this.tokensMap[envVar] = vaultCredentials
                } catch ( Exception ex ) {
                    this.step.echo "vaultpath: ${vaultPath} vaultKey ${vaultKey} appRole ${appRole}. error: ${ex.message}"
                    throw ex
                }

            }

            return this.tokensMap
        }
    }

    /**
     * @param path
     * @param vaultKey
     * @param envVar
     * @param appRole
     *
     * @return String
     */
    private def callVault(String path, String vaultKey, String envVar, String appRole) {
        def vaultCredentials = ""
        // define the secrets and the env variables
        def secrets = [
                [$class: 'VaultSecret', path: "${path}", secretValues: [
                        [$class: 'VaultSecretValue', envVar: "${envVar}", vaultKey: "${vaultKey}"],
                ]]
        ]

        // optional configuration, if you do not provide this the next higher configuration
        // (e.g. folder or global) will be used
        def configuration = [$class           : 'VaultConfiguration',
                             vaultUrl         : this.serviceDiscovery.locate('vault'),
                             vaultCredentialId: "${appRole}"]


        // inside this block your credentials will be available as env variables
        this.step.wrap([$class: 'VaultBuildWrapper', configuration: configuration, vaultSecrets: secrets]) {

            vaultCredentials = this.step.sh(script: "echo \$${envVar}", returnStdout: true).trim()
        }

        /** throw an exceptions when credential from Vault is empty  */
        if ("" == vaultCredentials) {
            throw new Exception("Vault value for `key:` {${envVar}} is empty.")
        }

        return vaultCredentials
    }
}
