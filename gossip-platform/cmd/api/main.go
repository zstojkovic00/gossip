package main

import (
	"context"
	"gossip-platform/internal/graphdb"
	"log"
	"net/http"
	"os"

	"github.com/neo4j/neo4j-go-driver/v5/neo4j"
)

func main() {
	driver, err := neo4j.NewDriverWithContext(
		os.Getenv("NEO4J_URI"),
		neo4j.BasicAuth(
			os.Getenv("NEO4J_USER"),
			os.Getenv("NEO4J_PASSWORD"),
			""))

	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := driver.Close(context.Background()); err != nil {
			log.Printf("closing neo4j driver: %v", err)
		}
	}()

	store := graphdb.NewStore(driver)
	log.Println("gossip-api listening on :8090")
	log.Fatal(http.ListenAndServe(":8090", routes(store)))

}
