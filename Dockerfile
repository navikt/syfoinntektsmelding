FROM navikt/java:8
COPY target/app.jar /app/

#ENV NAIS_SECRETS="/var/run/secrets/naisd.io/"
#ENV SRVDIALOGFORDELER_CERT_KEYSTORE="$NAIS_SECRETS/srvdialogfordeler_cert_keystore"

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
