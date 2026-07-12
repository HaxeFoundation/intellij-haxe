package debug.values;

import debug.Pointer;

import format.hl.Data.HLType;

/** A resolved location that can be written: its address and static HL type. */
typedef WriteTarget = {name:String, address:Pointer, type:HLType}
