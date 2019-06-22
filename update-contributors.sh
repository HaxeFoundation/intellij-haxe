#!/bin/bash

echo Recent contributors > CONTRIBUTORS.md
echo ----------------------------------- >> CONTRIBUTORS.md
git log --all --format=' - %cN <%cE>' --since=`date --date='last year' +%F`| sort -uf >> CONTRIBUTORS.md
echo >> CONTRIBUTORS.md
echo All contributors >> CONTRIBUTORS.md
echo ----------------------------------- >> CONTRIBUTORS.md
git log --all --format='- %cN <%cE>' | sort -uf >> CONTRIBUTORS.md
