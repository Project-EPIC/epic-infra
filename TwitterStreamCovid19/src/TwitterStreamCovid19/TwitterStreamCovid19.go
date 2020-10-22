package main

// OAuth1
import (
	"bufio"
	"gopkg.in/Shopify/sarama.v1"
	"io/ioutil"
	"log"
	"strconv"
	"net/url"
	"net/http"
	"os"
	"strings"
	"bytes"
	"encoding/json"
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

// Go though twitter OAuth process and get bearer token
func get_bearer_token(key string, secret string) string {
	client := &http.Client{}

	data := []byte("grant_type=client_credentials")
	req, err := http.NewRequest("POST", "https://api.twitter.com/oauth2/token", bytes.NewBuffer(data))
	if err != nil {
		log.Fatalf("Request config error: %s", err)
	}

	// Set Headers
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
	req.SetBasicAuth(key, secret)

	resp, err := client.Do(req)
	if err != nil {
		log.Fatalf("Cannot get Bearer token: %s", err)
	}

	bodyText, _ := ioutil.ReadAll(resp.Body)

	// Convert response body into JSON object
	var bodyJson map[string]interface{}
	json.Unmarshal(bodyText, &bodyJson)

	return bodyJson["access_token"].(string)
}

func stream_connect(ch chan int, token string, partition int) {
	// Set up for connection to twitter stream partition
	client := &http.Client{}

	var v = url.Values{}
	v.Set("partition", strconv.Itoa(partition))
	var streamUrl ="https://api.twitter.com/labs/1/tweets/stream/covid19?" + v.Encode()

	// Connect to Covid-19 Stream
	bearer := "Bearer " + token
	req, err := http.NewRequest("GET", streamUrl, nil)
	req.Header.Add("Authorization", bearer)
	 
	resp, err := client.Do(req)

	if err != nil {
		log.Fatalf("Error while connecting to twitter stream: %s", err)
		panic(err)
	}

	// Start reading the streaming response
	respbody := resp.Body
	scanner := bufio.NewScanner(respbody)
	buf := make([]byte, 8196) // Double default buffer size because some tweets were very large
	scanner.Buffer(buf, bufio.MaxScanTokenSize)
	scanner.Split(scanLines)

	// Get config for Kafka
	var kafkaServers = strings.Split(getEnv("KAFKA_SERVERS", "localhost:9092"), ",")
	var kafkaTopic = getEnv("KAFKA_TOPIC", "tweets-covid19")

	producer, err := sarama.NewAsyncProducer(kafkaServers, nil)

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

	defer func() {
		log.Printf("Partition %d is off", partition)
		respbody.Close()

		ch <- partition
		if err := producer.Close(); err != nil {
			log.Fatalln(err)
		}
	}()
	
	i := 0
	for {
		if !scanner.Scan() {
			// Retry connecting to the stream up to 3 times
			log.Printf("Error scanning partition %d: %s", partition, scanner.Err())
			panic("Partition down")
		}

		var tweet = scanner.Bytes()
		i++
		producer.Input() <- &sarama.ProducerMessage{Topic: kafkaTopic, Key: nil, Value: sarama.StringEncoder(tweet)}

		if i % 1000 == 0 {
			log.Printf("Partition %d: 1000 tweets received", partition)
			i = 0
		}
	}
}

func main() {
	var numPartitions = 4

	// Get config for Twitter Client
	var apiKey = os.Getenv("TWITTER_CONSUMER_API_KEY")
	var apiSecret = os.Getenv("TWITTER_CONSUMER_API_SECRET")

	// Get bearer token
	var token = get_bearer_token(apiKey, apiSecret)

	// Use a channel for restarting any droppped stream connections
	ch := make(chan int, numPartitions)

	// Connect to all stream partitions
	for partition:=1; partition <= numPartitions; partition++ {
		log.Printf("Starting covid connection to partition %d", partition)
		go stream_connect(ch, token, partition)
	}

	errCount := 0
	for {
		partition := <-ch
		errCount++
		if errCount % 10 == 0 {
			// Cool down before trying to reconnect to the stream partition
			log.Printf("%d panics occurred. Cooling down.", errCount) 
			time.Sleep(15 * time.Second)
		}

		log.Printf("Detected stream partition %d panic, will restart covid connection to partition %d\n", partition, partition)
        stream_connect(ch, token, partition)
	}
}
