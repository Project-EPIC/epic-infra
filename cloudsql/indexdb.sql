CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS event_index (
    id SERIAL PRIMARY KEY,
    filename TEXT,
    timestamp BIGINT,   -- ms since epoch
    start_index INTEGER,
    end_index INTEGER,
    event_id SERIAL REFERENCES events(id)
);