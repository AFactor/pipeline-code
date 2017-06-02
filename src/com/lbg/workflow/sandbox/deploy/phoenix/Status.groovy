package com.lbg.workflow.sandbox.deploy.phoenix

import groovy.json.JsonSlurperClassic

class Status implements Serializable {

    /**
     * output status
     */
    String status

    /**
     * output result
     */
    String result

    /**
     * output response id
     */
    String requestId

    Status() {
    }

    Status(String result) {
        def jsonOutput = (new HashMap(new JsonSlurperClassic().parseText(result))).asImmutable()
        this.status = jsonOutput.status
        this.result = jsonOutput.result
        this.requestId = jsonOutput.requestId
    }


    @Override
    String toString() {
        return "Status{" +
                "result='" + result + '\'' +
                "status='" + status + '\'' +
                "requestId='" + requestId + '\'' +
                '}'
    }
}
