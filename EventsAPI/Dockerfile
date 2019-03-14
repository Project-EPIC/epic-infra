FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/EventsAPI
 
ADD target/eventsapi.jar /home/EventsAPI/eventsapi.jar
ADD eventapi.yml /home/EventsAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "eventsapi.jar", "server", "config.yml"]