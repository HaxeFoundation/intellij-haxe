#!/bin/bash

# $1 is the required idea version.
# $2 is an optional ant target to build.

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to test'
    echo 'example: ./travis.sh 14.1.6'
    exit 1
fi


# Run the tests
./gradlew clean test -PtargetVersion="$1" $2

# Was our build successful?
stat=$?

# Return the build status
exit ${stat}
