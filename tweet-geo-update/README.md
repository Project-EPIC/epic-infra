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