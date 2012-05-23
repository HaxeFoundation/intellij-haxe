#if (neko && mydebug)
  typedef Vector = com.foo.SuperArray;// Only for "mydebug" mode on Neko
#end

class ConditionalCompilation {
  function foo() {
    #if debug
      trace("Debug infos for all debug compiles");
    #end

    #if flash8
      // Haxe code specific for flash player 8
    #elseif flash
      // Haxe code specific for flash platform (any version)
    #elseif js
      // Haxe code specific for javascript plaform
    #elseif neko
      // Haxe code specific for neko plaform
    #else
      // do something else
      #error // will display an error "Not implemented on this platform"
      #error "Custom error message" // will display an error "Custom error message"
    #end
  }
}