# Twitter streaming API Client

Client that watches a specific file for userids and connects to the filter Streaming API. 

All tweets are forwarded to an specified kafka queue.


## Run locally

Required: GoLang, `dep`

- `cd src/TwitterStreamFollow/ && GOPATH="$(pwd)/../../" dep ensure`
- `cp app.sh.template app.sh`
- Modify Twitter credentials
- `chmod +x app.sh`
- `./app.sh`

## Deploy to Docker

- `make push`