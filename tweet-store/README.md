# Tweet store service

Service that pulls tweets from Kafka, checks wether they belong to an specific event and store them in Google Cloud Storage.

## Run

Requirements: `mvn`, `make`, `java 8`.

- `make run`

## Deploy to DockerHub

Requirements: `mvn`, `make`, `java 8`, `docker`, logged in to dockerhub account with access to ProjectEPIC org. 

- `make push`

## Compile 

Requirements: `mvn`, `make`, `java 8`.

- `make`

## Output

This service generates gzipped files  to Google Cloud Storage. The format for the file is comprised of the following:

`yyyy/MM/dd/HH/`

## Configuration
### Environment variables

- `BATCH_SIZE`: Number of tweets to create a file.
- `KAFKA_TOPIC`: Topic where tweets need to be pulled from.
- `KAFKA_SERVER`: Servers where Kafka is hosted.
- `EVENT_NAME`: Name for event that the service needs to collect for.
- `BUCKET_NAME`: Name of the bucket where data needs to be saved.
- `KEYWORDS`: Keywords that compose the event. Helps decide wether a tweets if from the current event or not.