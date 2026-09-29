// Generates a `tracy/queue-v<N>.txt` table for one Tracy protocol version:
// the item sizes the client's own compiler computes, paired with the enum
// names in declaration order. Never type a table by hand - one misnumbered
// item silently misparses everything after it.
//
//   1. fetch public/common/TracyQueue.hpp (plus the headers it includes) of
//      the Tracy tag that speaks the version, into one directory;
//   2. cl /EHsc /std:c++17 /I<that directory> QueueTableProbe.cpp
//      (or g++/clang++ -std=c++17 -I<that directory>);
//   3. run the probe: one size per line, in QueueType order, NUM_TYPES excluded;
//   4. paste the enum's names (same order, `NUM_TYPES` dropped) in front of
//      the sizes as `Name;size` lines, LF endings, and save the file under
//      src/main/resources/tracy/ - TracyProtocolVersionTest pins its shape.
#include <stdio.h>
#include "TracyQueue.hpp"

int main() {
  const int count = sizeof(tracy::QueueDataSize) / sizeof(tracy::QueueDataSize[0]);
  for (int i = 0; i < count; i++) printf("%d\n", (int)tracy::QueueDataSize[i]);
  return 0;
}
