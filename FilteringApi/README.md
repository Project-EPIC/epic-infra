# FilteringAPI

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

### GET `/filtering/{event_name}/{keyword}`

List tweets for `event_name` event, filtered by `keyword` in tweet text and extended text.

**GET parameters**
- `count`: Number of tweets per page. Defaults to 100. Maximum 1000.
- `page`: Page number (gets specified page according to count). Defaults to 1

**Response (GET `/tweets/winter/colorado?page=1&count=1`)**
```json
{
  "tweets": [
    {
        "created_at":"Thu Apr 11 21:53:05 +0000 2019",
        "id":"1116459176685817856",
        "text":"The aftermath of the \u201Cbomb cyclone\u201D #colorado #spring �� @ Lakewood, Colorado https:\/\/t.co\/o4n57Bdof8",
        "extended_tweet":"",
        "profile_image":"https:\/\/pbs.twimg.com\/profile_images\/839185612984860672\/6KxqCaJG_normal.jpg",
        "filename":"gs:\/\/epic-collect\/winter\/2019\/04\/11\/21\/tweet-1555020000027-590.json.gz"
    }
  ],
  "meta": {
        "job_status":"DONE",
        "tweet_count":1,
        "total_count":48,
        "num_pages":48,  
        "count":1,   
        "page":1,
        "event_name":"winter",
        "keyword":"colorado"
  }
}
```
**Meta fields**

- `job_status`: Returns status value of the requested query job.
- `tweet_count`: Number of filtered tweets contained in the `$.tweets` array.
- `total_count`: Total number of filtered tweets for the current event.
- `num_pages`: Total number of available pages with the current count.
- `count`: Returns value for `count` parameter.
- `page`: Return value for `page` parameter.
- `event_name`: Returns normalized name for current event.
- `keyword`: Returns value for requested `keyword`.

**Status code**

- `200`: Tweets returned correctly in GET body
- `404`: No events with specified `event_name` or no tweets available under `event_name`.
- `503`: Issue connecting with Google Cloud Storage.


## Health Check

To see your applications health enter url `http://localhost:8081/healthcheck`

