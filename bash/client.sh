#!/bin/bash

export LC_ALL=en_US.UTF-8

# symlink stage
PARENT_DIR="$(dirname "$(dirname "$(realpath "$0")")")"
for f in "$PARENT_DIR"/src/*/*.java; do
    [[ -e "$f" ]] || continue
    ln -sf "$f" "$PARENT_DIR/$(basename "$f")"
done

# build and run
find src -name "*.java" > sources.txt
if javac -cp .:lib/* @sources.txt -d out/ 2>&1; then
    echo "build complete!"
    echo "starting client..."
    sleep 0.3
    
    java -cp out/:lib/* -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 client.GameClient "$USER"
else
    echo "build failed."
    exit 1
fi
