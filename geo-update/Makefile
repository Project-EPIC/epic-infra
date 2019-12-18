all: jar

TAG_CLIENT = 1.0.16
PROJECT_NAME = geo-update


run: jar
	java -jar target/geo-update.jar


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

jar: target/geo-update.jar

target/geo-update.jar: src/main/java/edu/colorado/cs/epic/geoupdate/App.java
	mvn package
	rm target/geo-update-*-javadoc.jar
	rm target/geo-update-*-sources.jar
	mv target/geo-update-*.jar target/geo-update.jar