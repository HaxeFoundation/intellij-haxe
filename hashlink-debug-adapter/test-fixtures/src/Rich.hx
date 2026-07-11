/**
 * One local per "rich" value kind, decoded by VariablesIntegrationTest:
 * arrays (bytes/object/dynamic flavours), Dynamic, enum, anonymous structure,
 * a closure, a captured-and-mutated local (compiler-boxed into a 1-element
 * array) and an explicit hl.Ref. All built from runtime values and used with
 * runtime indices on the print line, so the analyzer cannot fold them away.
 *
 * WARNING: line numbers are load-bearing test constants
 * (FIXTURE_RICH_LINE in DapIntegrationTestBase) — update them together.
 */
class Rich {
	public static function demo():Void {
		var n:Int = Std.parseInt("2"); // typed Int so [n, ...] is ArrayBytes_Int, not boxed ArrayObj
		var ints = [n, n + 3, n * 5];
		var names = ["a" + n, "b"];
		var dyn:Dynamic = n + 40;
		var shade = Shade.Tinted(n, "red");
		var anon = {width: n, tag: "t" + n};
		var captured = n * 10; // mutated by the closure below: genhl boxes it in a 1-element array
		var f = (a:Int, b:Int) -> { captured += a; return a + b; };
		var dynArray:Array<Dynamic> = [n, "s" + n];
		var byRef = hl.Ref.make(n); // a genuine HRef(i32) local pointing at n's slot
		Sys.println("rich:" + ints[n] + names[n - n] + Std.string(dyn) + Std.string(shade) + Std.string(anon) + f(n, n) + captured + dynArray[n - n] + byRef.get()); // FIXTURE_RICH_LINE = 23
	}
}
