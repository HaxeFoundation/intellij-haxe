#!/bin/bash

GrammarKitVersion="1.2.0"

    # Get our GrammarKit dependency
    if [ -f ~/Tools/GrammarKit-${GrammarKitVersion}.zip ];
    then
        cp ~/Tools/GrammarKit-${GrammarKitVersion}.zip . &&
        cp ~/Tools/light-psi-all-${GrammarKitVersion}.jar .
    else
        `wget -O GrammarKit-${GrammarKitVersion}.zip https://github.com/JetBrains/Grammar-Kit/releases/download/${GrammarKitVersion}/GrammarKit.zip`&&
        `wget -O light-psi-all-${GrammarKitVersion}.jar https://github.com/JetBrains/Grammar-Kit/releases/download/${GrammarKitVersion}/light-psi-all.jar` &&

        #Cache file locally except for Travis build which creates a new VM every time
        if [ "${TRAVIS}" != true ]; then
            mkdir -p ~/Tools &&
            cp GrammarKit-${GrammarKitVersion}.zip ~/Tools/ &&
            cp light-psi-all-${GrammarKitVersion}.jar ~/Tools/
        fi
    fi

    # Unzip GrammarKit
    unzip GrammarKit-${GrammarKitVersion}.zip
    mv GrammarKit/lib/grammar-kit.jar .
    mv light-psi-all-${GrammarKitVersion}.jar light-psi-all.jar

