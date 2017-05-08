package com.lbg.workflow.sandbox.deploy

class ServiceRuntimeBinary implements Serializable  {

    String artifact

    @Override
    String toString() {
        return "ServiceRuntimeBinary{" +
                "artifact='" + artifact + '\'' +
                '}'
    }
}
