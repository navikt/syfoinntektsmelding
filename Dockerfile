FROM navikt/java:11
COPY target/app.jar /app/

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=remote"
