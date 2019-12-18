FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/geo-update-api
 
ADD target/geoupdateapi.jar /home/geo-update-api/geoupdateapi.jar
ADD config.yml /home/geo-update-api/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "geoupdateapi.jar", "server", "config.yml"]
