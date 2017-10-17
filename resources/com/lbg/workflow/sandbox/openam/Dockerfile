FROM 10.112.159.88:40007/openam-liberty:14.0.0

COPY ob-cnf-access-mgmt-api*.zip /tmp/ob-cnf-access-mgmt-api.zip

RUN unzip /tmp/ob-cnf-access-mgmt-api.zip -d /app/ \
    && rm -f /tmp/ob-cnf-access-mgmt-api.zip

RUN echo '\
-DamCryptoDescriptor.provider=IBMJCE\n\
-DamKeyGenDescriptor.provider=IBMJCE\n\
-Djavax.xml.soap.MessageFactory=com.sun.xml.internal.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl\n\
-Djavax.xml.soap.SOAPFactory=com.sun.xml.internal.messaging.saaj.soap.ver1_1.SOAPFactory1_1Impl\n\
-Djavax.xml.soap.SOAPConnectionFactory=com.sun.xml.internal.messaging.saaj.client.p2p.HttpSOAPConnectionFactory\n\
-Djavax.xml.soap.MetaFactory=com.sun.xml.internal.messaging.saaj.soap.SAAJMetaFactoryImpl\n\
'> /app/wlp/usr/servers/ob-cnf-access-mgmt-api/jvm.options

ADD http://nexus.sandbox.extranet.group/nexus/content/repositories/binaries/lbg-bluemix-certs/lbgcacerts /app/wlp/usr/servers/ob-cnf-access-mgmt-api/resources/security/default_trust.jks

CMD ["/app/docker/docker-server", "run", "ob-cnf-access-mgmt-api"]