#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to build'
    echo 'example: ./build.sh 13.1.6'
    exit 1
fi

./fetchIdea.sh "$1"

#call the build script along with the path to a code package
#specific to the intellij version which we build against
if [ -d src/"$1" ]; then
    ant -f build.xml -Dversion.specific.code.location=src/"$1" -Dversion="$1"
else
    ant -f build.xml -Dversion="$1"
fi
