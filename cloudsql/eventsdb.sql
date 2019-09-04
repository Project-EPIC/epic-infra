CREATE TABLE events (
    name TEXT,
    normalized_name TEXT PRIMARY KEY,
    status TEXT,
    created_at TIMESTAMP,
    description TEXT,
    author VARCHAR(80)
);

CREATE TABLE event_activity (
  type varchar(20),
  time timestamp,
  author varchar(80),
  event_name text REFERENCES events(normalized_name)
);


CREATE TABLE keywords (
    event_name TEXT REFERENCES events(normalized_name),
    keyword TEXT
);
