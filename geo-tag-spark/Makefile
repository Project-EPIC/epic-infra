all: jar

PROJECT_NAME = GeoTagSpark
EPIC_JAR_BUCKET = epic-spark-jars

push: jar
	gsutil -m cp target/$(PROJECT_NAME).jar gs://$(EPIC_JAR_BUCKET)

clean:
	mvn clean
	rm -rf target/

jar: 
	mvn package

run:
	gcloud dataproc workflow-templates instantiate-from-file --file=workflow.yaml --region=global