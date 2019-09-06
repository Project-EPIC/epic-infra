FROM openjdk:8u121-jre-alpine

# Set this to the API folder path 
WORKDIR /home/API_NAME
 
# Replace API_NAME for the API_NAME from the Makefile
ADD target/API_NAME.jar /home/API_NAME/API_NAME.jar
ADD config.yml /home/API_NAME/config.yml
 
EXPOSE 8080 8081
 
# Replace API_NAME for the API_NAME from the Makefile
ENTRYPOINT ["java", "-jar", "API_NAME.jar", "server", "config.yml"]
