package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
)

func get[T any](read func(context.Context) (T, error)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		result, err := read(r.Context())
		if err != nil {
			log.Printf("%s %s: %v", r.Method, r.URL.Path, err)
			http.Error(w, "internal error", http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(result); err != nil {
			log.Printf("%s %s: encode: %v", r.Method, r.URL.Path, err)
		}
	}
}
