# Kubernetes cluster

Requirements: GCloud Kubernetes Engine cluster, `kubectl`

## Pre-Deploy instructions 

This needs to be done once when created the cluster only.

1. Upload Service account credentials: `./secrets/create_secret_keyfile.sh`
2. `cp /secrets.credentials.yml.template /secrets/credentials.yml`
3. Update `/secrets/credentials.yml` with user and password to access your Google Cloud managed Postgres DB (see [../cloudsql](../cloudsql) to create tables and obtain user/password).
4. `cp /secrets/twsecret.yml.template /secrets/twsecret.yml`
5. Update `/secrets/twsecret.yml` with your Twitter application access token, secret, etc. You can access your Twitter apps [here](https://developer.twitter.com/en/apps). Your group may have an application already created, ask your lab mates or advisor first. 
6. Deploy secrets to cluster: `kubectl create -f secrets/`
7. Deploy Kafka cluster: `kubectl create -f kafka/`
8. Deploy twitter stream:  `kubectl create -f twitter-stream/`
9. Deploy dashboard services: `kubectl create -f api/`
10. Configure access inside of cluster: `kubectl create -f auth.yaml`
11. Create Kubernetes ingress: `kubectl create -f ingress.yaml` and SSL certificate `kubectl create -f cert.yaml`

## Acess Kafka Manager service

1. `kubectl proxy`
2. Open browser to [http://localhost:8001/api/v1/namespaces/default/services/kafka-manager:http/proxy/](http://localhost:8001/api/v1/namespaces/default/services/kafka-manager:http/proxy/)
