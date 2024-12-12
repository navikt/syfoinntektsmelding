FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/*.jar ./

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
