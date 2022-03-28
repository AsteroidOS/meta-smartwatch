#!/bin/bash
SRC="$1"
DST="$2"

function showUsage {
    echo "Usage:"
    echo " ./new-watch.sh SOURCE-CODENAME NEW-CODENAME"
    echo "Example:"
    echo " ./new-watch.sh ray beluga"
    exit 1
}

if [ $# -eq 0 ]; then
    echo "No arguments supplied."
    showUsage
elif [ -z "$SRC" ]; then
    echo "No source layer to copy from."
    showUsage
elif [ -z "$DST" ]; then
    echo "No target defined."
    showUsage
fi

if [ ! -d "meta-$SRC/" ]; then
    echo "Source layer (meta-$SRC) doesn't exist!"
    exit
fi

if [ -d "meta-$DST/" ]; then
    echo "Destination layer (meta-$DST) already exists!"
    exit
fi

cp -r meta-$SRC/ meta-$DST/

FILES=$(find meta-$DST/ -name "*$SRC*")

for f in $FILES
do
    echo mv "$f" "${f/$SRC/$DST}"
done

find meta-$DST/ -type f -exec sed -i "s/"$SRC"/$DST/g" {} \;

git add "meta-$DST/"