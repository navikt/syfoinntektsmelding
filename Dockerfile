FROM navikt/java:11
COPY syfoinntektsmelding-core/build/libs/*.jar ./

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
