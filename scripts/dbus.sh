#!/bin/bash

DIR=$(dirname $0)
LIBS="$DIR/../build/lib-run"
NATIVE="$DIR/../"
MSGS="$DIR/../msg-classes"

if [ ! -d "$LIBS" ]; then
	echo "Please run 'gradle createRuntime'"
	exit 1
fi

CLASSPATH="$LIBS/*:$MSGS"

exec java -cp "$CLASSPATH" -Djava.library.path="$NATIVE" "$@"
