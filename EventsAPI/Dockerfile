FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/EventsAPI
 
ADD target/eventsapi-0.1.jar /home/EventsAPI/eventsapi-0.1.jar
ADD eventapi.yml /home/EventsAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "eventsapi-0.1.jar", "server", "config.yml"]