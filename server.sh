
#!/bin/bash
find src -name "*.java" > sources.txt
if javac -cp .:lib/* @sources.txt -d out/ 2>&1; then
	echo "build complete!"
	echo "starting server..."
	pid=$(lsof -ti :4267) && { echo "Process found on port 4267 (PID: $pid). Killing it..."; kill $pid; }
	java -cp out/:lib/* server.GameServer
else
	echo "build failed."
	exit 1
fi
