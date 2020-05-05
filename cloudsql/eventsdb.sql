CREATE TABLE events (
    name TEXT,
    normalized_name TEXT PRIMARY KEY,
    status TEXT,
    created_at TIMESTAMP,
    description TEXT,
    author VARCHAR(80)
);

CREATE TABLE event_activity (
  type VARCHAR(20),
  time TIMESTAMP,
  author VARCHAR(80),
  event_name TEXT REFERENCES events(normalized_name)
);


CREATE TABLE keywords (
    event_name TEXT REFERENCES events(normalized_name),
    keyword TEXT
);

CREATE TABLE follows (
    event_name TEXT REFERENCES events(normalized_name),
    follow TEXT
);

CREATE TABLE covid19 (
    event_name TEXT REFERENCES events(normalized_name)
);
