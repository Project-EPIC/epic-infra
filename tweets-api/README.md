# TweetsAPI

## Development

**Requirements**: `mvn`, `gloud` cli (logged in to Project EPIC GCloud project).


1. `make run`
1. To check that your application is running enter url `http://localhost:8080`



## Building new docker image

**Requirements**: `mvn`, `docker`

1. `make build`


## Pushing new docker image

**Requirements**: `mvn`, `docker`, access to [cloud.docker.com/u/projectepic/](https://cloud.docker.com/u/projectepic/) from your dockerhub account.

1. Edit `Makefile` and update `TAG_CLIENT` variable to new version
1. `make push`


## Endpoints

### GET `/tweets/{event_name}/`

List tweets for `event_name` event.

**GET parameters**
- `count`: Number of tweets per page. Defaults to 100. Maximum 1000.
- `page`: Page number (gets specified page according to count). Defaults to 1


**Response (GET `/tweets/winter/`)**
```json
{
  "tweets": [
    {
      "created_at": "Thu Apr 11 06:08:50 +0000 2019",
      "id": 1,
    }
  ],
  "meta": {
    "tweet_count": 1,
    "refreshed_time": "Sat Apr 13 01:43:59 GMT 2019",
    "total_count": 48572,
    "num_pages": 48572,
    "count": 1,
    "event_name": "winter",
    "page": 3
  }
}
```
**Meta fields**

- `tweet_count`: Number of tweets contained in the `$.tweets` array.
- `refreshed_time`: Files are indexed once every 10 minutes. This fields points to the time when the index was updated.
- `total_count`: Total number of tweets for the current event.
- `num_pages`: Total number of available pages with the current count.
- `count`: Returns value for `count` parameter.
- `event_name`: Returns normalized name for current event.
- `page`: Return value for `page` parameter.

**Status code**

- `200`: Tweets returned correctly in GET body
- `400`: Page requested is higher than available pages.
- `404`: No events with specified `event_name` or no tweets available under `event_name`.
- `503`: Issue connecting with Google Cloud Storage.

### GET `/tweets/{event_name}/counts`

Calculate number of tweets for event identified by `event_name`.

**GET parameters**
- `bucket`: Granularity to aggregate tweet counts. Available: `hour`, `day`,`month`. Default `hour`.
- `page`: Page number (gets specified page according to count). Defaults to 1

**Response (GET `/tweets/winter/counts`)**
```json

{
  "meta": {
    "bucket": "hour",
    "refreshed_time": "Sat Apr 13 01:54:16 GMT 2019",
    "event_name": "winter"
  },
  "tweets": {
    "Thu Apr 11 07:00:00 GMT 2019": 1000,
    "Thu Apr 11 09:00:00 GMT 2019": 1000,
    "Thu Apr 11 11:00:00 GMT 2019": 1000,
  }
}
```

**Meta fields**

- `refreshed_time`: Files are indexed once every 10 minutes. This fields points to the time when the index was updated.
- `bucket`: Returns value for `bucket` parameter.
- `event_name`: Returns normalized name for current event.

**Status code**

- `200`: Counts returned correctly in GET body.
- `404`: No events with specified `event_name` or no tweets available under `event_name`.
- `503`: Issue connecting with Google Cloud Storage.


## Health Check


To see your applications health enter url `http://localhost:8081/healthcheck`

