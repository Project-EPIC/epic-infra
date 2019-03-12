# Twitter streaming API Client

Client that watches a specific file for keywords and connects to the filter Streaming API. 

All tweets are forwarded to an specified kafka queue.


## Run locally

Required: GoLang, `dep`

- `cd src/TwitterStream/ && GOPATH="$(pwd)/../../" dep ensure`
- `cp app.sh.template app.sh`
- Modify Twitter credentials
- `chmod +x app.sh`
- `./app.sh`

## Deploy to Docker

- `make push`