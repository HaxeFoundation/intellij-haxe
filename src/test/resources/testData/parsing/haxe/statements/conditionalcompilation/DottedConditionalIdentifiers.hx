#if flash.flash8
// Haxe code specific for flash player 8
#elseif flash.flash
// Haxe code specific for flash platform (any version)
#elseif js.target
// Haxe code specific for javascript plaform
#else
// do something else
  #error // will display an error "Not implemented on this platform"
  #error "Custom error message" // will display an error "Custom error message"
#end

#if my.debug
class Crazy {
  #else
  interface Crazy {
  #end
// yep
}

class ConditionalCompilation {
  #if !my.debug inline #end
public function abc() {}
}

#if (vm.neko && my.debug)
class FooNeko {

}
// Only for "mydebug" mode on Neko
#end

#if (flash || php.something)
// Code that works for either flash or PHP
class FooFlash {

}
#end

#if (vm.neko && my.debug)
typedef Vector = com.foo.SuperArray;// Only for "mydebug" mode on Neko
#end

class ConditionalCompilation {
  function foo() {
    #if (vm.neko && php) || (neko && my.debug)
    trace("Debug infos for all debug compiles");
    #end
  }

  #if vm.neko inline #elseif sys.cpp private #else public #end function foo2() {}
}