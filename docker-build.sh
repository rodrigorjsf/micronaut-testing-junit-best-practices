#!/bin/sh
if [ "$1" = "graalvm" ]; then
    docker build -f Dockerfile.graalvm . -t demo
else
    docker build . -t demo
fi
echo
echo
echo "To run the docker container execute:"
echo "    $ docker run -p 8080:8080 demo"
