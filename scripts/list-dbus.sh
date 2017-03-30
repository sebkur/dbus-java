#!/bin/bash

DIR=$(dirname $0)
CMD="$DIR/dbus.sh"
CLASS="org.freedesktop.dbus.bin.ListDBus"

exec "$CMD" "$CLASS" "$@"
