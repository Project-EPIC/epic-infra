CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS geo_tag_index (
    id SERIAL PRIMARY KEY,
    geo_hash TEXT,
    created_at TEXT,
    username TEXT,
    user_id_str TEXT,
    tweet_id_str TEXT,
    lang TEXT,
    source TEXT,
    in_reply_to_user_id_str TEXT,
    text TEXT,
    image_link TEXT,
    is_retweet BOOLEAN,
    event_id SERIAL REFERENCES events(id),
    UNIQUE(event_id, tweet_id_str)
);