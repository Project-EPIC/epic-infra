FROM openjdk:8u121-jre-alpine
 
WORKDIR /home/MentionsAPI
 
ADD target/mentionsapi.jar /home/MentionsAPI/mentionsapi.jar
ADD config.yml /home/MentionsAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "mentionsapi.jar", "server", "config.yml"]