#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to build'
    echo 'example: ./build.sh 13.1.6'
    exit 1
fi

./gradlew buildPlugin -PideaVersion="$1"

