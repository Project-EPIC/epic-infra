package main

import (
	"fmt"
	//"strings"
	//"net/http"
	//"io/ioutil"
	"math/rand"
	"encoding/json"
)
import (
	"context"
	//"reflect"
	"github.com/segmentio/kafka-go"
	"github.com/dghubble/oauth1"
)


func KafkaWriter(x string, w *kafka.Writer) {
	i :=rand.Intn(10)
	w.WriteMessages(context.Background(),
		kafka.Message{
			Key:   []byte(string(i)),
			Value: []byte(x),
		},
	)
}



func main() {

	config := oauth1.NewConfig("H3EOhNsWcM6lzAIPhfhcgWe0I", "WmeXG9VN1PCa7aRl3OYZ5gczW8RO6xOdiofOhQYaOu0Wx46trx")
    token := oauth1.NewToken("2456854800-ZxqMTVE0NUsSxh7sh8uzB4vzR4FpRbHdybvFLdy", "gPrKGEmQK7pgycI7flKzWq6jUkeU84zQIF9tM8ekBMpQD")
	url := "https://stream.twitter.com/1.1/statuses/filter.json?track=trump"

	httpClient := config.Client(oauth1.NoContext,token)
	resp, _ := httpClient.Get(url)
    
	decoder:=json.NewDecoder(resp.Body)
	//w := kafka.NewWriter(kafka.WriterConfig{
	//	Brokers: []string{"34.73.54.57:9092"},
	//	Topic:   "MyTopic",
	//})
	
	for {
		var myJson interface{}
		decoder.Decode(&myJson)
		b, err:=json.MarshalIndent(myJson, "", "    ")
		if err==nil{
			fmt.Println(string(b))
			//go KafkaWriter(string(b),w)
		}
	}

}