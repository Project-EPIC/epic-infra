FROM openjdk:8u121-jre-alpine

WORKDIR /var/kubernetes-api
EXPOSE 8080 8081
ADD target/kubernetes-api.jar /var/kubernetes-api/kubernetes-api.jar
ADD config.yml /var/kubernetes-api/config.yml
ENTRYPOINT ["java", "-jar", "kubernetes-api.jar", "server", "config.yml"]