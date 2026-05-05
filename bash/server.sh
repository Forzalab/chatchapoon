#!/bin/bash

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
	pid=$(lsof -ti :4267) && { echo "Process found on port 4267 (PID: $pid). Killing it..."; kill $pid; }
	sleep 0.3
	java -cp out/:lib/* server.GameServer
else
	echo "build failed."
	exit 1
fi
