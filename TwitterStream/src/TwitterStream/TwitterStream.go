package main

// OAuth1
import (
	"bufio"
	"github.com/dghubble/oauth1"
	"gopkg.in/Shopify/sarama.v1"
	"io/ioutil"
	"log"
	"net/url"
	"os"
	"strings"
	"time"
)


// Break tweets arriving from stream
func scanLines(data []byte, atEOF bool) (advance int, token []byte, err error) {
	if atEOF && len(data) == 0 {
		return 0, nil, nil
	}
	if i := strings.Index(string(data), "\r\n"); i >= 0 {
		// We have a full '\r\n' terminated line.
		return i + 2, data[0:i], nil
	}
	// If we're at EOF, we have a final, non-terminated line. Return it.
	if atEOF {
		return len(data), dropCR(data), nil
	}
	// Request more data.
	return 0, nil, nil
}

func dropCR(data []byte) []byte {
	if len(data) > 0 && data[len(data)-1] == '\n' {
		return data[0 : len(data)-1]
	}
	return data
}

// Get env variables with fallback
func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

// Get keywords from file
func getKeywords() string {
	var fileKeywords = getEnv("FILE_KEYWORDS", "keywords.txt")
	var keywords, err = ioutil.ReadFile(fileKeywords)
	if err != nil {
		log.Fatalf("File reading error: %s", err)
		panic(err)
	}
	return string(keywords)
}

func main() {
	// Get config for Twitter Client
	var apiKey = os.Getenv("TWITTER_CONSUMER_API_KEY")
	var apiSecret = os.Getenv("TWITTER_CONSUMER_API_SECRET")
	var accessToken = os.Getenv("TWITTER_ACCESS_TOKEN_KEY")
	var accessSecret = os.Getenv("TWITTER_ACCESS_TOKEN_SECRET")

	// Get config for Kafka
	var kafkaServers = strings.Split(getEnv("KAFKA_SERVERS", "localhost:9092"), ",")
	var kafkaTopic = getEnv("KAFKA_TOPIC", "tweets")

	// Prepare Oauth1 client
	var conf = oauth1.NewConfig(apiKey, apiSecret)
	var token = oauth1.NewToken(accessToken, accessSecret)
	var client = conf.Client(oauth1.NoContext, token)

	// Prepare keywords
	var keywords = getKeywords()
	for keywords == "" {
		log.Printf("No keywords detected. Sleeping 1 minute and trying to pull new keywords.")
		time.Sleep(1 * time.Minute)
		keywords = getKeywords()
	}
	var v = url.Values{}
	v.Set("track", keywords)
	log.Printf("Tracking keywords: %s", keywords)
	var streamUrl = "https://stream.twitter.com/1.1/statuses/filter.json?" + v.Encode()

	// Connect to Streaming API
	resp, err := client.Post(streamUrl, "application/json", nil)

	if err != nil {
		log.Fatalf("Error while connecting to twitter: %s", err)
		panic(err)
		return
	}

	if resp.StatusCode != 200 {
		log.Fatalf("Error while connecting to twitter, status code returned: %d", resp.StatusCode)
		panic(err)
		return
	}
	respbody := resp.Body
	scanner := bufio.NewScanner(respbody)
	scanner.Split(scanLines)

	producer, err := sarama.NewAsyncProducer(kafkaServers, nil)

	defer func() {
		if err := producer.Close(); err != nil {
			log.Fatalln(err)
		}
	}()

	if err != nil {
		log.Fatalf("Error while bootstraping Kafka producer: %s", err)
		panic(err)
		return
	}

	go func() {
		for err := range producer.Errors() {
			log.Fatalf("Error: %s", err)
			os.Exit(2)
		}
	}()

	var i = 1
	for {
		if !scanner.Scan() {
			log.Printf("Error scanning: %s", scanner.Err())
			break
		}
		i = i + 1
		var tweet = scanner.Bytes()

		// Check if keywords have been updated
		if i%10 == 0 {
			var newKeywords = getKeywords()
			i = 1

			for newKeywords == "" {
				log.Printf("No keywords detected. Sleeping 1 minute and trying to pull new keywords.")
				time.Sleep(1 * time.Minute)
				newKeywords = getKeywords()
			}

			if keywords == newKeywords {
				continue
			}

			err = respbody.Close()

			if err != nil {
				log.Fatalf("Error closing: %s", err)
				panic(err)
				return
			}

			var v = url.Values{}
			v.Set("track", newKeywords)
			log.Printf("Changing keywords to %s", newKeywords)

			streamUrl = "https://stream.twitter.com/1.1/statuses/filter.json?" + v.Encode()
			resp, err := client.Post(streamUrl, "application/json", nil)

			if err != nil {
				log.Fatalf("Error while connecting to Twitter: %s", err)
				panic(err)
				return
			}
			respbody  = resp.Body

			// Check if error is bc of cooling down. Waiting and retrying!

			if resp.StatusCode == 420 {
				log.Printf("Cooling down... %d", resp.StatusCode)
				err = respbody.Close()

				if err != nil {
					log.Fatalf("Error closing: %s", err)
					panic(err)
					return
				}

				time.Sleep(2 * time.Minute)
				resp, err = client.Post(streamUrl, "application/json", nil)
				log.Printf("Cooling down...Done: %d", resp.StatusCode)
				respbody = resp.Body
			}

			if resp.StatusCode != 200 {
				var body, err = ioutil.ReadAll(respbody)
				log.Printf(string(body))
				log.Fatalf("Error while connecting to twitter, status code returned: %d", resp.StatusCode)
				panic(err)
				return
			}

			scanner = bufio.NewScanner(respbody)
			scanner.Split(scanLines)
			keywords = newKeywords

		}

		if len(tweet) == 0 {
			// empty keep-alive
			log.Printf("Staying alive... %d", i)
			continue
		}

		producer.Input() <- &sarama.ProducerMessage{Topic: kafkaTopic, Key: nil, Value: sarama.StringEncoder(tweet)}
		log.Printf("Tweet received")

	}

	log.Printf("Closing")

}
