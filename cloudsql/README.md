# Managed Postgres Instance
In order to maintain state we use a managed Postgres instance in Google Cloud. You can create it [here](https://console.cloud.google.com/sql/instances) Our instance at Project EPIC had the following configuration:

- Database version: _PostgreSQL 9.6_
- Region:_us-central1_
- Zone: _us-central1-c_
- Instance ID: _epic-event_
- Private and public IP enabled
- Memory: _3.75 Gb_
- CPU: _1 vCPU_
- Storage: _10 GB in SSD_
- No backups
- Single zone availability (reduces costs)

Remember to set the password and user (postgres by default) in the *[corresponding Kubernetes secret](../kubernetes#pre-deploy-instructions)*.

## Connect to DB

```
gcloud sql connect epic-event --user=postgres --quiet
```

## DB and table creation

1. Create databases
```
CREATE DATABASE annotationsdb;
CREATE DATABASE eventsdb;
```
2. Create annotations tables (see [annotationsdb.sql](annotationsdb.sql))
3. Create events tables (see [eventsdb.sql](eventsdb.sql))

