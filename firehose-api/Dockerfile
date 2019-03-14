FROM openjdk:8u121-jre-alpine

WORKDIR /var/firehose-api
EXPOSE 8080 8081
ADD target/firehose-api.jar /var/firehose-api/firehose-api.jar
ADD config.yml /var/firehose-api/config.yml
ENTRYPOINT ["java", "-jar", "firehose-api.jar", "server", "config.yml"]