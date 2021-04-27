# Basert på https://github.com/spotify/docker-kafka/blob/master/kafka/Dockerfile


# Kafka and Zookeeper

FROM openjdk:11.0.6-jre

ENV DEBIAN_FRONTEND noninteractive
ENV SCALA_VERSION 2.13
ENV KAFKA_VERSION 2.7.0
ENV KAFKA_HOME /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"

# Install Kafka, Zookeeper and other needed things
RUN apt-get update
RUN apt-get install -y  --allow-unauthenticated zookeeper wget supervisor dnsutils
RUN rm -rf /var/lib/apt/lists/*
RUN apt-get clean
RUN wget -q http://apache.mirrors.spacedump.net/kafka/"$KAFKA_VERSION"/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz -O /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz
RUN tar xfz /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz -C /opt
RUN rm /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz

ADD start-kafka.sh /usr/bin/start-kafka.sh
RUN chmod +x /usr/bin/start-kafka.sh


# jobb rundt en rar newline-greie
RUN echo "" >> $KAFKA_HOME/config/server.properties
# sett opp kafka til å lytte på localhost
RUN echo "advertised.listeners=PLAINTEXT://localhost:9092" >> $KAFKA_HOME/config/server.properties
RUN echo "listeners=PLAINTEXT://0.0.0.0:9092" >> $KAFKA_HOME/config/server.properties

# Supervisor config
ADD kafka.conf zookeeper.conf /etc/supervisor/conf.d/

# 2181 is zookeeper, 9092 is kafka
EXPOSE 2181 9092

CMD ["supervisord", "-n"]

