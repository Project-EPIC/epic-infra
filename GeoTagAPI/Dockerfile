FROM openjdk:8u121-jre
 
WORKDIR /home/GeoTagAPI
 
ADD target/geotagapi.jar /home/GeoTagAPI/geotagapi.jar
ADD config.yml /home/GeoTagAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "geotagapi.jar", "server", "config.yml"]
