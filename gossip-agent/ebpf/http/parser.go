package http

import (
	"strconv"
	"strings"
)

type Request struct {
	Method string
	URL    string
}

type Response struct {
	Status int
}

func ParseRequest(msg string) (Request, bool) {
	line := strings.SplitN(msg, "\r\n", 2)[0]
	parts := strings.Fields(line)
	if len(parts) < 3 {
		return Request{}, false
	}
	switch parts[0] {
	case "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "CONNECT":
		return Request{Method: parts[0], URL: parts[1]}, true
	}
	return Request{}, false
}

func ParseResponse(msg string) (Response, bool) {
	line := strings.SplitN(msg, "\r\n", 2)[0]
	parts := strings.Fields(line)
	if len(parts) < 2 || !strings.HasPrefix(parts[0], "HTTP/") {
		return Response{}, false
	}
	code, err := strconv.Atoi(parts[1])
	if err != nil {
		return Response{}, false
	}
	return Response{Status: code}, true
}
