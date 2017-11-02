package com.lbg.workflow.sandbox.deploy

class Service implements Serializable {

    String name
    String type
    String scm
    boolean deploy
    ServiceRuntime runtime

    HashMap tokens
    HashMap platforms

    Service() {
    }


    @Override
    String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", scm='" + scm + '\'' +
                ", deploy='" + deploy + '\'' +
                ", runtime=" + runtime +
                ", tokens=" + tokens +
                ", platforms=" + platforms +
                '}'
    }
}
