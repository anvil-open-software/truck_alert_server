#!/usr/bin/env bash

code=`/bin/curl --head --silent --show-error --write-out '%{http_code}' --output /tmp/head http://127.0.0.1:8080/handshakes`
[[ $code -eq 200 ]] || { echo "execution failed $code"; echo /tmp/head; exit 1; }
