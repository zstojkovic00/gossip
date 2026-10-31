#!/usr/bin/env bash

(
  printf "IP|KONTEJNER|MREZA\n"
  docker inspect $(docker ps -q) --format '{{$name := .Name}}{{range $net, $conf := .NetworkSettings.Networks}}{{$conf.IPAddress}}|{{$name}}|{{$net}}
{{end}}' | sed 's#|/#|#' | sort -t'|' -k1,1V
) | column -t -s '|'
