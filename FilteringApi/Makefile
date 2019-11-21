all: jar

TAG_CLIENT = 1.0.31
PROJECT_NAME = filteringapi


run: jar
	java -jar target/$(PROJECT_NAME).jar server config.yml


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
	rm -rf target/

jar: target/$(PROJECT_NAME).jar

target/$(PROJECT_NAME).jar: src/main/java/edu/colorado/cs/epic/filteringapi/* src/main/java/edu/colorado/cs/epic/filteringapi/*/*
	mvn package
	mv target/$(PROJECT_NAME)-*.jar target/$(PROJECT_NAME).jar
