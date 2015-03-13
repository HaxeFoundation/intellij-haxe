#!/bin/bash

./fetchIdea.sh

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
