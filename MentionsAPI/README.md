# MentionsAPI

## Development

**Requirements**: `mvn`, `gloud` cli (logged in to Project EPIC GCloud project).

How to start the Mentions application
---

1. Run `mvn package` to build your application
1. Start application with `java -jar target/MentionsAPI-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`

## Endpoints

### GET `/mentions/{event_name}/`

List tweets for `event_name` event.

**GET parameters**
- `page`: Page number (gets specified page according to count). Defaults to 1
- `count`: Number of mentioned users per page. Defaults to 100. Maximum 1000.




**Response (GET `/mentions/winter/`)**
```json
{
  "mentions": [
    {
      "user_id": 520318548,
      "user": [
        "john_smith"
      ],
      "times_mentioned": 41875,
      "total_retweets": 0,
      "total_likes": 0
    }
  ],
  "meta": {
    "count": 100,
    "event_name": "winter",
    "page": 1
  }
}
```
**Meta fields**


- `user_id`: Unique identifier of the mentioned user.
- `user`: Set of screen names of the mentioned user.
- `times_mentioned`: Number of times the user was mentioned during the event.
- `total_retweets`: Total number of retweets of the mentioned user.
- `total_likes`: Total number of likes of the mentioned user.
- `count`: Returns value for `count` parameter.
- `event_name`: Returns normalized name for current event.
- `page`: Return value for `page` parameter.

**Status code**

- `200`: Mentions returned correctly in GET body
- `400`: Page requested is higher than available pages.
- `404`: No events with specified `event_name` or no mentions available under `event_name`.
- `503`: Issue connecting with Google Cloud Storage.
