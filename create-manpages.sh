#!/bin/bash

DIR="manpages"
PDF="$DIR/pdf"
HTML="$DIR/html"
MAN="$DIR/man"

mkdir -p "$PDF"
mkdir -p "$HTML"
mkdir -p "$MAN"

# PDF
for f in legacy/*.sgml; do
	docbook2pdf -o "$PDF" $f
done

# HTML
for f in legacy/*.sgml; do
	l=$(basename $f .sgml)
	docbook2html -o "$HTML" $f
	mv "$HTML/index.html" "$HTML/$l.html"
done

# MAN
for f in legacy/*.sgml; do
	l=$(basename $f .sgml)
	docbook2x-man "$f"
	mv DBUS-JAVA.1 "$MAN/$l.man"
done
