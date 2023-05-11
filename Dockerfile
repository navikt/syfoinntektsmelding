FROM ghcr.io/navikt/baseimages/temurin:17
COPY build/libs/*.jar ./

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
