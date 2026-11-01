package main

import (
	"net/http"

	"gossip-platform/internal/graphdb"
)

func routes(store *graphdb.Store) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/v1/topology", get(store.Topology))
	return mux
}
