all: build
push: push
.PHONY: push build

TAG = 1.0.1
PROJECT_NAME = twitter-stream-covid19

build: main ca-certificates.crt
	docker build -t projectepic/$(PROJECT_NAME) .
	docker tag projectepic/$(PROJECT_NAME) projectepic/$(PROJECT_NAME):$(TAG)

push: build
	docker push projectepic/$(PROJECT_NAME)
	docker push projectepic/$(PROJECT_NAME):$(TAG)

main: src/TwitterStreamCovid19/TwitterStreamCovid19.go
	cd src/TwitterStreamCovid19/ && GOPATH="$$(pwd)/../../" dep ensure
	cd src/TwitterStreamCovid19/ && GOPATH="$$(pwd)/../../" CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o ../../main TwitterStreamCovid19.go

mainmac: src/TwitterStreamCovid19/TwitterStreamCovid19.go
	cd src/TwitterStreamCovid19/ && GOPATH="$$(pwd)/../../" dep ensure
	cd src/TwitterStreamCovid19/ && GOPATH="$$(pwd)/../../" CGO_ENABLED=0 GOOS=darwin go build -a -installsuffix cgo -o ../../mainmac TwitterStreamCovid19.go

runmac: mainmac
	./mainmac

run: mainmac
	./main

ca-certificates.crt:
	curl --remote-name --time-cond cacert.pem https://curl.haxx.se/ca/cacert.pem
	mv cacert.pem ca-certificates.crt

clean:
	docker rmi projectepic/$(PROJECT_NAME):$(TAG) || :
	docker rmi projectepic/$(PROJECT_NAME) || :
	rm ca-certificates.crt
	rm main
	rm mainmac

