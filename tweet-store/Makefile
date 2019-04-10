all: jar

TAG_CLIENT = 1.0.8
PROJECT_NAME = tweet-store


run: jar
	java -jar target/tweet-store.jar


build: jar
	docker build -t projectepic/$(PROJECT_NAME) .
	docker tag projectepic/$(PROJECT_NAME) projectepic/$(PROJECT_NAME):$(TAG_CLIENT)

push: build
	docker push projectepic/$(PROJECT_NAME)
	docker push projectepic/$(PROJECT_NAME):$(TAG_CLIENT)

clean:
	docker rmi projectepic/$(PROJECT_NAME):$(TAG) || :
	docker rmi projectepic/$(PROJECT_NAME) || :
	rm -rf target/

jar: target/tweet-store.jar

target/tweet-store.jar: src/main/java/edu/colorado/cs/epic/App.java
	mvn package
	rm target/tweet-store-*-javadoc.jar
	rm target/tweet-store-*-sources.jar
	mv target/tweet-store-*.jar target/tweet-store.jar
