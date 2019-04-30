# Connect to DB

```
gcloud sql connect epic-event --user=postgres --quiet
```

## Event Activity table

```sql

CREATE TABLE event_activity (
type varchar(20),
time timestamp,
author varchar(80),
event_name text REFERENCES events(normalized_name)
);
```

## Event table

```sql
```
