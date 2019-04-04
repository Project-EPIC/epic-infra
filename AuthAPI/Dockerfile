FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/AuthAPI
 
ADD target/authapi.jar /home/AuthAPI/authapi.jar
ADD config.yml /home/AuthAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "authapi.jar", "server", "config.yml"]