/**
 * One local per "rich" value kind, decoded by VariablesIntegrationTest:
 * arrays (bytes + object flavours), Dynamic, enum, anonymous structure and a
 * closure. All built from runtime values and used with runtime indices on the
 * print line, so the analyzer cannot fold them away.
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
		var f = (a:Int, b:Int) -> a + b;
		Sys.println("rich:" + ints[n] + names[n - n] + Std.string(dyn) + Std.string(shade) + Std.string(anon) + f(n, n)); // FIXTURE_RICH_LINE = 19
	}
}
