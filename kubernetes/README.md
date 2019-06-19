# Deploy cluster

Requirements: GCloud Kubernetes Engine cluster, `kubectl`

## Deploy

1. `kubectl create -f kafka/`
1. `kubectl create -f firehose/`
3. `kubectl create -f api/`
4. `kubectl create -f auth.yaml`
5. `kubectl create -f ingress.yaml`

## Acess Kafka Manager service

1. `kubectl proxy`
2. Open browser to [http://localhost:8001/api/v1/namespaces/default/services/kafka-manager:http/proxy/](http://localhost:8001/api/v1/namespaces/default/services/kafka-manager:http/proxy/)
