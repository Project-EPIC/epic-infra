FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/FilteringAPI
 
ADD target/filteringapi.jar /home/FilteringAPI/filteringapi.jar
ADD config.yml /home/FilteringAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "filteringapi.jar", "server", "config.yml"]
