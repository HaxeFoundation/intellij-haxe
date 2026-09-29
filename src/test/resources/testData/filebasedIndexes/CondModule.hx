package com.example;

// Module file with no class matching the file name: CondExtra is ancillary,
// so its full FQN carries the module segment (com.example.CondModule.CondExtra)
// and the indexer also emits the short form (com.example.CondExtra).
// The #if keeps the file out of stub indexing.

#if sys
class CondSysOnly {}
#end

class CondExtra {
  public function new() {}
  public function extraMethod():Void {}
}

var condModuleVar:Int = 1;

function condModuleFunc():Void {}
