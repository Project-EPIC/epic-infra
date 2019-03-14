#!/usr/bin/env bash

gcloud iam service-accounts keys create ./keyfile.json --iam-account twitter-server-uploads@crypto-eon-164220.iam.gserviceaccount.com
kubectl create secret generic keyfile --from-file=keyfile.json=./keyfile.json