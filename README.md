# EPIC Infrastructure

EPIC Infrastructure backend mono repo. Contains all services, kubernetes deployment files, dataproc definitions...

## Diagram

![EPIC Infrastructure Diagram](epic_infra.png)

## Overview

This project is prepared to run on top of a pre-configured Kubernetes cluster. Basic knowledge of Kubernetes is required. A good start is this [Udacity course](https://www.udacity.com/course/scalable-microservices-with-kubernetes--ud615). The project is structured into 2 separate parts: collection pipeline services and dashboard services. 

### Collection Pipeline Services

This side is in charge of connecting to Twitter and collecting tweets 24/7. It has 2 services: [_TwitterStream_](/TwitterStream) (downloads tweets and sends to Kafka) and [_tweet-store_](/tweet-store) (receives tweets from Kafka and uploads them to Google Cloud Storage). 

Tweets are stored following a `EVENT/YYYY/MM/DD/HH/` folder structure (a tweet received at 2PM on April 3rd 2019 for event _winter_ would be stored in the folder `winter/2019/04/03/14`) in a the `epic-collect` Google Cloud bucket. Each tweet received is buffered. When the buffer reaches size of 1000 tweets a file is created and uploaded in the corresponding folder.


------------
# Requirements
List of requirements for deploying or developing in this repository.

## Development

In order to work on the services you will need the following:
- Installed java 8. (Ex: `brew install java8`)
- Installed maven. (Ex: `brew install mvn`)
- Installed Make (Ex: `brew install make`)
- Install our authlib: `cd authlib && mvn install`.
- Set up your local Maven installation to pull from GitHub repository (read how to do so [here](https://help.github.com/en/articles/configuring-apache-maven-for-use-with-github-package-registry#authenticating-to-github-package-registry))
- Log in on your GCloud CLI. `gcloud auth login`
- Create a default token using your GCloud user. `gcloud auth application-default login`


## Deploying

In order to deploy you will need:
- Docker CLI installed (Ex: `brew install docker`)
- A [hub.docker.com](https://hub.docker.com/) account and your Docker CLI connected to it (`docker login`)
- Editor access to [Project EPIC Docker Hub organization](https://cloud.docker.com/orgs/projectepic/teams).
- Editor access to the GCloud project.
- `kubectl` installed (Ex: `brew install kubectl`)
- `kubectl` connected to the corresponding cluster (Project EPIC: `gcloud container clusters get-credentials epic-prod --zone us-central1-c --project crypto-eon-164220`)
------
# New service

## Start development

Requirements: [Development requirements](#development)
- Read [Getting started guide for DropWizard](https://www.dropwizard.io/1.3.14/docs/getting-started.html)
- `mvn archetype:generate -DarchetypeGroupId=io.dropwizard.archetypes -DarchetypeArtifactId=java-simple -DarchetypeVersion=1.3.9`
- Add AuthLib as a dependency (follow instructions [here](authlib/#install-on-service))
- Add authentification on your service (follow instructions [here](authlib/#install-on-service))
- Add CORS configuration (see instructions [here](authlib#cors-specification))
- Add Makefile to service (you can copy from the [Makefile template](templates/Makefile)
- Add Dockerfile to service (you can copy from the [Dockerfile template](templates/Dockerfile)
- Add root resource under resources folder and register it on the application (you can copy root resource from the [EventsAPI example](EventsAPI/src/main/java/edu/colorado/cs/epic/eventsapi/resource/RootResource.java)
- Add `config.yml` file copying from [this template](templates/config.yml)
- Set up your configuration to retrieve production key-value (see [this example](MediaAPI/src/main/java/edu/colorado/cs/epic/mediaapi/MediaAPIConfiguration.java))
- Set up your application to get [configuration parameters from environment variables](https://www.dropwizard.io/0.8.0/docs/manual/core.html#environment-variables)
- Run project locally: `make run`

## Deployment

- (ONLY FIRST TIME) Create new Kubernetes [definition file](templates/api.yml) in the [api folder](kubernetes/api)
- Make sure your resources are protected with the right annotations (see how to do it [here](authlib#protect-resources))
- Make sure you have [health checks](https://www.dropwizard.io/0.8.0/docs/manual/core.html#health-checks) configured properly for external dependencies
- Update image version in `Makefile`
- Create and upload docker image: `make deploy`
- Update docker image version in your [api definition file](kubernetes/api)
- `kubectl replace -f api/NEW.yml` (replace NEW with your api file name)


------
# System deployment

- Create managed Postgres instance (see [cloudsql instructions](./cloudsql))
- Create Dataproc workflow (see [dataproc instructions](./dataproc))
- Create a Kubernetes cluster and deploy services (see [kubernetes instructions](./kubernetes))

------

## Frequent errors

**Google Cloud is giving an authorization error on local**

- Log in on your GCloud CLI. `gcloud auth login`
- Make sure you have been added to the proper Google Cloud project.
- Create a default token using your GCloud user. `gcloud auth application-default login`
- Make sure you don't have any *GOOGLE_APPLICATION_CREDENTIALS* environment variable set.
