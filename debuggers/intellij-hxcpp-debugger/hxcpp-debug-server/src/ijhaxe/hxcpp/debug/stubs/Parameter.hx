package ijhaxe.hxcpp.debug.stubs;

#if !cpp

/**
	Non-cpp stand-in for `cpp.vm.Debugger.Parameter`, selected by the
	conditional typedef in DebuggerApi (the cpp package is rejected on other
	targets). Keep the API surface identical to the std class; the cpp compile
	of any fixture cross-checks shared code against the real type.
**/
class Parameter {
	public var name(default, null):String;
	public var value(default, null):Dynamic;

	public function new(name:String, value:Dynamic) {
		this.name = name;
		this.value = value;
	}
}
#end
