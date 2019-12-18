# Tweet GEO update

Script that updates GEO JSON objects found in tweets, by stringifying any occurances of place/bounding_box attribute.

## Run

Requirements: `mvn`, `make`, `java 8`.

- `make run`

## Deploy to DockerHub

Requirements: `mvn`, `make`, `java 8`, `docker`, logged in to dockerhub account with access to ProjectEPIC org. 

- `make push`

## Compile 

Requirements: `mvn`, `make`, `java 8`.

- `make`

## Input/Output

This service receives a JSON file of tweets and returns another copy of the file but with updated GEO JSON attributes.

## Note
Work is still in progress for this service.
To do:
- add an endpoint for updating historical/finished events
- add an endpoint for updating an array of 1000 tweets for updating active events
- deploy endpoints


- References:
https://cloud.google.com/storage/docs/gsutil/addlhelp/HowSubdirectoriesWork
https://stackoverflow.com/questions/33690136/how-to-create-an-empty-folder-in-google-cloud-bucket-using-java-api
