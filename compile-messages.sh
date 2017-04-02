#!/bin/bash

mkdir -p core/msg-classes

cd translations;
for i in *.po; do
	l=$(basename $i .po)
	msgfmt --verbose --java2 -r dbusjava_localized -d ../core/msg-classes -l $l $i;
done
cd ..
msgfmt --verbose --java2 -r dbusjava_localized -d core/msg-classes translations/en_GB.po
