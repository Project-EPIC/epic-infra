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
# Deployment

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
