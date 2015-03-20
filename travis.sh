#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to test'
    echo 'example: ./travis.sh 13.1.6'
    exit 1
fi

./fetchIdea.sh "$1"

# Run the tests
if [ "$1" = "-d" ]; then
    ant -d -f build-test.xml -DIDEA_HOME=./idea-IU
else
    ant -f build-test.xml -DIDEA_HOME=./idea-IU
fi

# Was our build successful?
stat=$?

if [ "${TRAVIS}" != true ]; then
    ant -f build-test.xml -q clean
    rm -rf idea-IU
fi

# Return the build status
exit ${stat}
