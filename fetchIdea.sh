#!/bin/bash
if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to fetch'
    echo 'example: ./fetchIdea.sh 13.1.6'
    exit 1
fi
# call ant tasks that downloads and extracts IDEA source files
ant -f fetchIdea.xml -Dversion="$1"

