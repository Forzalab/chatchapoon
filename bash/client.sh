#!/bin/bash

# symlink stage
P="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
find "$P/src" -mindepth 2 -maxdepth 2 -type f -exec ln -sf -t "$P" {} + 2>/dev/null
find "$P/sh" -mindepth 1 -maxdepth 2 -type f ! -name "${BASH_SOURCE[0]##*/}" -exec ln -sf -t "$P" {} + 2>/dev/null

# build and run
find src -name "*.java" > sources.txt
if javac -cp .:lib/* @sources.txt -d out/ 2>&1; then
	echo "build complete!"
	echo "starting client..."
	sleep 0.3
	java -cp out/:lib/* client.GameClient
else
	echo "build failed."
	exit 1
fi
