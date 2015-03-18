#!/bin/bash

./fetchIdea.sh

ant -f build.xml -DIDEA_HOME=./idea-IU
