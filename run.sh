#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <Base URL>"
  exit 1
fi

export PWD=$(pwd) && docker build . -t blur/backend && docker run -d -e BASE_URL=$1 -p 4567:4567 \
  --mount type=volume,dst=/usr/src/blur/log,volume-driver=local,volume-opt=type=none,volume-opt=o=bind,volume-opt=device=${PWD}/log --rm blur/backend

# https://linux.die.net/man/ (asks for javascript and cookies)
# https://www.man7.org/linux/man-pages/index.html
# https://www.kernel.org/doc/man-pages/
# https://ubuntu.com/
# https://ubuntu.com/blog/
# https://pt.wikipedia.org/wiki/Linux
