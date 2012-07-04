#if flash8
// Haxe code specific for flash player 8
#elseif flash
// Haxe code specific for flash platform (any version)
#elseif js
// Haxe code specific for javascript plaform
#else
// do something else
  #error // will display an error "Not implemented on this platform"
  #error "Custom error message" // will display an error "Custom error message"
#end

#if mydebug
class Crazy {
  #else
  interface Crazy {
  #end
// yep
}

class ConditionalCompilation {
  #if !debug inline #end
public function abc() {}
}

#if (neko && mydebug)
class FooNeko {

}
// Only for "mydebug" mode on Neko
#end

#if (flash || php)
// Code that works for either flash or PHP
class FooFlash {

}
#end

#if (neko && mydebug)
typedef Vector = com.foo.SuperArray;// Only for "mydebug" mode on Neko
#end

class ConditionalCompilation {
  function foo() {
    #if (neko && php) || (neko && mydebug)
    trace("Debug infos for all debug compiles");
    #end
  }
}