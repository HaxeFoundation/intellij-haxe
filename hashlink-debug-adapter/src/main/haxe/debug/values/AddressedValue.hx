package debug.values;

import debug.Pointer;

import format.hl.Data.HLType;

/**
 * An addressable slot: a debuggee memory address and the static HL type stored
 * there. Produced when resolving a container child (object field, array element,
 * ...) to the exact location a read or write should land. A WriteTarget is this
 * plus the child's name.
 */
typedef AddressedValue = {address:Pointer, type:HLType}
