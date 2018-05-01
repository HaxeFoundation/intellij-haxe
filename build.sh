#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to build'
    echo 'example: ./build.sh 2016.3.4'
    exit 1
fi

./gradlew clean buildPlugin -PtargetVersion="$1" --info

