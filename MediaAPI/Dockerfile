FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/MediaAPI
 
ADD target/mediaapi.jar /home/MediaAPI/mediaapi.jar
ADD config.yml /home/MediaAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "mediaapi.jar", "server", "config.yml"]
