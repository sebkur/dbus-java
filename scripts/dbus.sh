#!/bin/bash

DIR=$(dirname $0)
LIBS="$DIR/../exe/build/lib-run"
MSGS="$DIR/../msg-classes"

if [ ! -d "$LIBS" ]; then
	echo "Please run 'gradle createRuntime'"
	exit 1
fi

CLASSPATH="$LIBS/*:$MSGS"

exec java -cp "$CLASSPATH" "$@"
