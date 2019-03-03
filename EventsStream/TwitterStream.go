package main

import (
	"fmt"
	"strings"
	//"net/http"
	//"io/ioutil"
	"math/rand"
	"encoding/json"
	"os"
	"context"
	//"reflect"
	"github.com/segmentio/kafka-go"
	"github.com/dghubble/oauth1"
)


func KafkaWriter(x []string, w *kafka.Writer) {
	i :=rand.Intn(10)
	for j:=0; j<len(x);j++{
		w.WriteMessages(context.Background(),
			kafka.Message{
				Key:   []byte(string(i)),
				Value: []byte(x[j]),
			},
		)	
	}
	
}



func main() {
	config := oauth1.NewConfig(os.Getenv("TWITTER_CONSUMER_API_KEY"),os.Getenv("TWITTER_CONSUMER_API_SECRET"))
    token := oauth1.NewToken(os.Getenv("TWITTER_ACCESS_TOKEN_KEY"), os.Getenv("TWITTER_ACCESS_TOKEN_SECRET"))
	url := "https://stream.twitter.com/1.1/statuses/filter.json?track="
	keywords :=os.Args[1:]
	httpClient := config.Client(oauth1.NoContext,token)
	keywordsString :=strings.Join(keywords,",")
   	url = url+keywordsString
	resp, _ := httpClient.Get(url)
	decoder:=json.NewDecoder(resp.Body)
	w := kafka.NewWriter(kafka.WriterConfig{
		Brokers: []string{os.Getenv("KAFKA_BROKER")},
		Topic:   os.Getenv("KAFKA_TOPIC"),
	})
	fmt.Println(url)
	batchSize:=1000
	for {
		tweetList:=make([]string, 1000)
		for i := 0; i < batchSize; i++ {
			var myJson interface{}
			decoder.Decode(&myJson)
			tweet,_:=json.MarshalIndent(myJson,"", "	")
			tweetList = append(tweetList,string(tweet))
		}
		go KafkaWriter(tweetList,w)
	}
}