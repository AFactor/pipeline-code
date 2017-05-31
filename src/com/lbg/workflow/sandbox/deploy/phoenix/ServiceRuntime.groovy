package com.lbg.workflow.sandbox.deploy.phoenix

class ServiceRuntime implements Serializable {

    ServiceRuntimeBinary binary

    @Override
    String toString() {
        return "ServiceRuntime{" +
                "binary=" + binary +
                '}'
    }
}
