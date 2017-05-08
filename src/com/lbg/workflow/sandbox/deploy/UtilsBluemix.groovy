package com.lbg.workflow.sandbox.deploy

import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def buildBluemixEnv(HashMap bluemix) {
    def envs = [
            "BM_API"   : "${bluemix.api}",
            "BM_DOMAIN": "${bluemix.domain}",
            "BM_ORG"   : "${bluemix.org}",
            "BM_ENV"   : "${bluemix.env}",
            "DISK"     : "${bluemix.disk}",
            "MEMORY"   : "${bluemix.memory}"
    ]
    return envs
}

@NonCPS
def buildServiceBluemixEnv(HashMap serviceBluemix, HashMap globalBluemix) {
    def envs = buildBluemixEnv(globalBluemix)
    if (serviceBluemix != null && serviceBluemix.disk && !serviceBluemix.disk.empty) {
        envs["DISK"] = serviceBluemix.disk
    }
    if (serviceBluemix != null && serviceBluemix.memory && !serviceBluemix.memory.empty) {
        envs["MEMORY"] = serviceBluemix.memory
    }
    return envs
}

@NonCPS
def buildServiceBluemix(HashMap serviceBluemix, HashMap globalBluemix) {
    def envs = globalBluemix
    if (serviceBluemix != null) {
        for (e in serviceBluemix) {
            envs[e.key] = e.value
        }
    }
    return envs
}


@NonCPS
def toWithEnv(map) {
    def envs = []
    for (e in map) {
        envs << "${e.key}=${e.value}"
    }
    return envs
}
