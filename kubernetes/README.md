# Deploy cluster

Requirements: GCloud Kubernetes Engine cluster, `kubectl`

## Deploy

1. `kubectl create -f kafka/`
1. `kubectl create -f firehose/`
3. `kubectl create -f api/`
4. `kubectl create -f auth.yaml`
5. `kubectl create -f ingress.yaml`
