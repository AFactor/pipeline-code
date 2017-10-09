package com.lbg.workflow.sandbox.deploy

class ServiceRuntime implements Serializable {

    ServiceRuntimeBinary binary
    ServiceRuntimeBinary config

    @Override
    String toString() {
        return "ServiceRuntime{" +
                "binary=" + binary +
                ", config=" + config +
                '}'
    }
}
