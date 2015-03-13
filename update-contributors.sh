#!/bin/bash

echo Recent contributors > CONTRIBUTORS.md
echo ----------------------------------- >> CONTRIBUTORS.md
git log --all --format=' - %cN <%cE>' --since=`date --date='last year' +%F`| sort -u >> CONTRIBUTORS.md
echo >> CONTRIBUTORS.md
echo All contributors >> CONTRIBUTORS.md
echo ----------------------------------- >> CONTRIBUTORS.md
git log --all --format='- %cN <%cE>' | sort -u >> CONTRIBUTORS.md
