#!/bin/bash

PORT=$(grep "public static final int PORT =" ./src/shared/Protocol.java | sed 's/[^0-9]*//g')

if [ -z "$PORT" ]; then
    echo "no port def"
    exit 1
fi

# symlink stage
#!/usr/bin/env bash
PARENT_DIR="$(dirname "$(dirname "$(realpath "$0")")")"
for f in "$PARENT_DIR"/src/*/*.java; do
    [[ -e "$f" ]] || continue
    ln -sf "$f" "$PARENT_DIR/$(basename "$f")"
done

# build and run
find src -name "*.java" > sources.txt
if javac -cp .:lib/* @sources.txt -d out/ 2>&1; then
	echo "build complete!"
	echo "starting server..."
    pid=$(lsof -ti :$PORT) && { echo "Process found on port $PORT (PID: $pid)."; kill $pid; }
	sleep 0.3
	java -cp out/:lib/* server.GameServer
else
	echo "build failed."
	exit 1
fi
