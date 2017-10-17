#!/bin/bash

set +ex

function deployApp() {
    docker stop ${APP}
    docker rm ${APP}
    docker rmi ${APP}

    set -ex

    docker build -t 10.112.159.88:40007/${APP} .

    docker run -d -p ${APP_PORT}:9080 -e JAVA_HOME="/opt/ibm/java/jre/" -e OPENAM_SERVER_URL="http://${APP_HOSTNAME}:${APP_PORT}" -e OPENAM_DOMAIN="localhost" -e OPENAM_PASSWORD="password" -e OPENAM_COOKIE_DOMAIN=${APP_HOSTNAME} -e OPENAM_SSL_OPTS="-DamCryptoDescriptor.provider=IBMJCE -DamKeyGenDescriptor.provider=IBMJCE" -e OPENAM_LB_OPTS=" " --name ${APP} 10.112.159.88:40007/${APP}

    sleep 180

    docker exec -i ${APP} sh -c "\
    sed -i -e \"s|authentication-url=.*|authentication-url=$AUTHENTICATION_API_URL|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|adp-url=.*|adp-url=$ADP_API_URL|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|ard-url=.*|ard-url=$ARD_API_URL|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|payment-service-url=.*|payment-service-url=$PAYMENT_SERVICE_API_URL|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|default-trust-store=.*|default-trust-store=$OUTBOUND_TLS_TRUSTSTORE|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|default-trust-store-password=.*|default-trust-store-password=$OUTBOUND_TLS_TRUSTSTORE_PASSWORD|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|default-key-store=.*|default-key-store=$OUTBOUND_TLS_KEYSTORE|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties && \
    sed -i -e \"s|default-key-store-password=.*|default-key-store-password=$OUTBOUND_TLS_KEYSTORE_PASSWORD|\" /app/wlp/usr/servers/ob-cnf-access-mgmt-api/bootstrap.properties \
    "

    docker exec -i ${APP} sh -c 'cd /app/wlp/dev/tools/openam && chmod +x register_module.sh && ./register_module.sh'
    sleep 5

    docker restart  ${APP}

    sleep 30

}