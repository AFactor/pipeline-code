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