all: jar

TAG_CLIENT = 1.0.16
PROJECT_NAME = tweet-geo-update


run: jar
	java -jar target/tweet-geo-update.jar


build: jar
	docker build -t projectepic/$(PROJECT_NAME) .
	docker tag projectepic/$(PROJECT_NAME) projectepic/$(PROJECT_NAME):$(TAG_CLIENT)

push: build
	docker push projectepic/$(PROJECT_NAME):latest
	docker push projectepic/$(PROJECT_NAME):$(TAG_CLIENT)

clean:
	docker rmi projectepic/$(PROJECT_NAME):$(TAG) || :
	docker rmi projectepic/$(PROJECT_NAME) || :
	mvn clean

jar: target/tweet-geo-update.jar

target/tweet-geo-update.jar: src/main/java/edu/colorado/cs/epic/App.java
	mvn package
	rm target/tweet-geo-update-*-javadoc.jar
	rm target/tweet-geo-update-*-sources.jar
	mv target/tweet-geo-update-*.jar target/tweet-geo-update.jar
