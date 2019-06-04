FROM openjdk:8u121-jre
 
WORKDIR /home/TweetsAPI
 
ADD target/tweetsapi.jar /home/TweetsAPI/tweetsapi.jar
ADD config.yml /home/TweetsAPI/config.yml
 
EXPOSE 8080 8081
 
ENTRYPOINT ["java", "-jar", "tweetsapi.jar", "server", "config.yml"]
