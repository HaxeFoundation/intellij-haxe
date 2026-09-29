class Foo {
	function f():Void {
		#if js
		trace("js", 1 + 2);
		var badly = {a: 1, b: 2};
		#else
		trace("other");
		#end
		var x = #if js 1 + 1 #else 2 * 3 #end;
		var y = 1 #if true < #else > #end 2;
	}
}
