all: jar

run: jar
	java -jar target/tweet-store.jar

jar: target/tweet-store.jar

target/tweet-store.jar: src/main/java/edu/colorado/cs/epic/App.java
	mvn package
	rm target/tweet-store-*-javadoc.jar
	rm target/tweet-store-*-sources.jar
	mv target/tweet-store-*.jar target/tweet-store.jar
