CREATE TABLE tweets (
    tweet_id text NOT NULL PRIMARY KEY,
    tweet text NOT NULL
);

CREATE TABLE annotation (
    tweet_id text NOT NULL REFERENCES tweets(tweet_id),
    tag text NOT NULL,
    event_name text NOT NULL,
    authuser text,
    PRIMARY KEY (tweet_id, tag, event_name)
);
