package graphdb

import "github.com/neo4j/neo4j-go-driver/v5/neo4j"

func list[T any](r *neo4j.Record, key string, toItem func(map[string]any) T) []T {
	v, _ := r.Get(key)
	rows, _ := v.([]any)
	items := make([]T, 0, len(rows))
	for _, row := range rows {
		m, _ := row.(map[string]any)
		items = append(items, toItem(m))
	}
	return items
}

func value[T any](m map[string]any, key string) T {
	t, _ := m[key].(T)
	return t
}

func optional[T any](m map[string]any, key string) *T {
	t, ok := m[key].(T)
	if !ok {
		return nil
	}
	return &t
}
