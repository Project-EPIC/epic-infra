# EventsAPI

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

### GET `/events/`

List all events from Database.

**Response**
```json
[
  {
    "name": "bombcyclone2019",
    "keywords": [
      "snow",
      "cyclone",
      "#bombcyclone",
      "colorado",
      "floods"
    ],
    "description": "Tracking bomb cyclone from Colorado happened March 13th",
    "normalized_name": "bombcyclone2019",
    "status": "NOT_ACTIVE",
    "created_at": 1552643251592,
    "created_at_str": "2019-03-15 03:47:31.592"
  },
  {
    "name": "name 2",
    "keywords": [
      "kasdas1",
      "kasdasd2"
    ],
    "description": "d2",
    "normalized_name": "name-2",
    "status": "NOT_ACTIVE",
    "created_at": 1552884762193,
    "created_at_str": "2019-03-17 22:52:42.193"
  }
 ]
```
**Status code**

- `200`: Events returned correctly in GET body
- `500`: Issue connecting with database

### POST `/events/`

Create new event.

**POST body**
```json
{
    "name": "bombcyclone2019",
    "description": "Tracking bomb cyclone from Colorado happened March 13th",
	"keywords": ["floods", "snow","colorado", "#bombcyclone","cyclone"]
}
```

**Response**
```json
  {
    "name": "bombcyclone2019",
    "keywords": [
      "snow",
      "cyclone",
      "#bombcyclone",
      "colorado",
      "floods"
    ],
    "description": "Tracking bomb cyclone from Colorado happened March 13th",
    "normalized_name": "bombcyclone2019",
    "status": "ACTIVE",
    "created_at": 1552643251592,
    "created_at_str": "2019-03-15 03:47:31.592"
  }
```
**Status code**

- `201`: Event created and started on database. Includes link to event unique URL in header.
- `409`: Conflict. There's already another event with the same name.
- `500`: Issue connecting with database.
- `503`: Kubernetes error. Issue syncing the infrastructure. Event will be created but marked as failed if it failed to start it.

### GET `/events/{normalizedName}`

Retrieve information for event identified by `normalizedName`.

**Response (GET `/events/bombcyclone2019`)**
```json

  {
    "name": "bombcyclone2019",
    "keywords": [
      "snow",
      "cyclone",
      "#bombcyclone",
      "colorado",
      "floods"
    ],
    "description": "Tracking bomb cyclone from Colorado happened March 13th",
    "normalized_name": "bombcyclone2019",
    "status": "NOT_ACTIVE",
    "created_at": 1552643251592,
    "created_at_str": "2019-03-15 03:47:31.592"
  }
```
**Status code**

- `200`: Event returned correctly in GET body.
- `404`: No event existing with `normalizedName`.
- `500`: Issue connecting with database.

### PUT `/events/{normalizedName}/{ACTIVE,NOT_ACTIVE,FAILED}`

Set status for event identified by `normalizedName`. Only status available:

- `ACTIVE`
- `NOT_ACTIVE`
- `FAILED`

**Response (PUT `/events/bombcyclone2019/NOT_ACTIVE`)**
```json

  {
    "name": "bombcyclone2019",
    "keywords": [
      "snow",
      "cyclone",
      "#bombcyclone",
      "colorado",
      "floods"
    ],
    "description": "Tracking bomb cyclone from Colorado happened March 13th",
    "normalized_name": "bombcyclone2019",
    "status": "NOT_ACTIVE",
    "created_at": 1552643251592,
    "created_at_str": "2019-03-15 03:47:31.592"
  }
```
**Status code**

- `202`: Event status updated. Includes link to event unique URL in header.
- `404`: No event existing with `normalizedName`.
- `400`: Status has not been recognized. Make sure you are using `ACTIVE` or `NOT_ACTIVE` or `FAILED` on the URL.
- `500`: Issue connecting with database.
- `503`: Kubernetes error. Issue syncing the infrastructure. Event will be created but marked as failed if it failed to start it.


## Tasks

You can execute tasks on admin port (8081).

### POST `/tasks/sync`

Check Database and update Kubernetes deployments according to it.

**Status code**

- `200`: All synced.
- `500`: Error syncing. Check response body.



## Health Check


To see your applications health enter url `http://localhost:8081/healthcheck`

