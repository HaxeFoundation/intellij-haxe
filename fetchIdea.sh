#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to fetch'
    echo 'example: ./fetchIdea.sh 13.1.6'
    exit 1
fi

ideaVersion=$1

# Get our IDEA dependency
if [ -f ~/Tools/ideaIU-${ideaVersion}.tar.gz ];
then
    cp ~/Tools/ideaIU-${ideaVersion}.tar.gz .
else
    wget http://download.jetbrains.com/idea/ideaIU-${ideaVersion}.tar.gz && mkdir -p ~/Tools && cp ideaIU-${ideaVersion}.tar.gz ~/Tools/ideaIU-${ideaVersion}.tar.gz
fi

# Unzip IDEA
tar zxf ideaIU-${ideaVersion}.tar.gz
rm -rf ideaIU-${ideaVersion}.tar.gz

# Move the versioned IDEA folder to a known location
ideaPath=$(find . -name 'idea-IU*' | head -n 1)
mv ${ideaPath} ./idea-IU
