#!/bin/bash

set +ex

function deployApp() {
    docker stop ${APP}
    docker rm ${APP}
    docker rmi ${APP}

    set -ex

    docker build -t 10.112.159.88:40007/${APP} .

    docker run -d -p ${APP_PORT}:9080 \
    -p ${OPENAM_DIRECTORY_PORT}:${OPENAM_DIRECTORY_PORT} \
    -p ${OPENAM_DIRECTORY_ADMIN_PORT}:${OPENAM_DIRECTORY_ADMIN_PORT} \
    -p ${OPENAM_DIRECTORY_JMX_PORT}:${OPENAM_DIRECTORY_JMX_PORT} \
    -e JAVA_HOME="/opt/ibm/java/jre/" \
    -e OPENAM_SERVER_URL="http://${APP_HOSTNAME}:${APP_PORT}" \
    -e OPENAM_DOMAIN="${APP_HOSTNAME}" \
    -e OPENAM_PASSWORD="${OPENAM_PASSWORD}" \
    -e OPENAM_COOKIE_DOMAIN="${OPENAM_COOKIE_DOMAIN}" \
    -e OPENAM_SSL_OPTS="${OPENAM_SSL_OPTS}" \
    -e OPENAM_LB_OPTS="${OPENAM_LB_OPTS}" \
    -e OPENAM_LB_PRIMARY_URL="http://${APP_HOSTNAME}:${APP_PORT}/access-mgmt-service" \
    -e OPENAM_LIBERTY_TRUSTSTORE="${OPENAM_LIBERTY_TRUSTSTORE}" \
    -e OPENAM_LIBERTY_KEYSTORE="${OPENAM_LIBERTY_KEYSTORE}" \
    -e OPENAM_LIBERTY_TRUSTSTORE_PASSWD="${OPENAM_LIBERTY_TRUSTSTORE_PASSWD}" \
    -e OPENAM_LIBERTY_KEYSTORE_PASSWD="${OPENAM_LIBERTY_KEYSTORE_PASSWD}" \
    -e AM_CRYPTO_DESCRIPTOR_PROVIDER="${AM_CRYPTO_DESCRIPTOR_PROVIDER}" \
    -e AM_KEYGEN_DESCRIPTOR_PROVIDER="${AM_KEYGEN_DESCRIPTOR_PROVIDER}" \
    -e OPENAM_DIRECTORY_PORT="${OPENAM_DIRECTORY_PORT}" \
    -e OPENAM_DIRECTORY_ADMIN_PORT="${OPENAM_DIRECTORY_ADMIN_PORT}" \
    -e OPENAM_DIRECTORY_JMX_PORT="${OPENAM_DIRECTORY_JMX_PORT}" \
    --name ${APP} 10.112.159.88:40007/${APP}

    sleep 200

    docker exec -i ${APP} sh -c 'find /app -name bootstrap.properties | xargs cat' >  bootstrap.properties
    docker exec -i ${APP} sh -c 'set' >  environment.properties
    docker exec -i ${APP} sh -c 'cd /app/wlp/dev/tools/openam && chmod +x configure_openam.sh && ./configure_openam.sh '  || exit_code=1
    docker exec -i ${APP} sh -c 'find /app/wlp -name install.log | xargs cat ' > install.log
    sleep 5

    docker restart  ${APP}

    sleep 30
}