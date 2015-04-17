#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo 'This script must be called with the version of IDEA to fetch'
    echo 'example: ./fetchIdea.sh 13.1.6'
    exit 1
fi

ideaVersion=$1
ideaPrimaryVersionID=${ideaVersion%%\..*}

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

# Disable unneeded plugins.  Really, those that cause problems.
# The configuration goes into the user's home directory, in to
# a hidden directory, named for the version of Idea being run.
echo Disabling unneeded or problematic plugins...
idea_dir=${HOME}/.IntelliJIdea${ideaPrimaryVersionID}
config_dir=${idea_dir}/config
dp_file=${config_dir}/disabled_plugins.txt
[[ -d ${idea_dir} ]] || mkdir ${idea_dir}
[[ -d ${config_dir} ]] || mkdir ${config_dir}
echo "Git4Idea" >> ${dp_file}

