# Twitter Spark job to extract all tweets with Geo Tags
This spark job reads and processes all JSON user geo objects. It will on look at tweets that have 
`coordinates.coordinates` or `place.bounding_box`. For these tweets, the spark job will generate a quad key based on the coordinates and extract the fields user, user_id_str, created_at, tweet_id_str, lang, source, in_reply_to_user_id_str, text, col.media_url_https AS image_link, and reweeted_status.

## To test
1. Build and push new jar to GCP bucket: `make push` 
2. Run dataproc job: `gcloud dataproc workflow-templates instantiate-from-file --file=workflow.yaml --region=global`

## To deploy Spark job change
1. Build and push new jar to GCP bucket: `make push`

