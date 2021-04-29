FROM navikt/java:11
COPY build/libs/*.jar ./

COPY init.sh /init-scripts/init.sh

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
