#!/bin/bash

pushd hxcpp-debugger-protocol/src
haxe -lib debugger -java JavaProtocol -main JavaProtocol
popd
