package;

interface Renderer {
	// bodiless signature: wrapped parameters continue ONE step
	function drawEverythingWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
		fifth:Int):Void;
}

@:keep class TodoChecks extends BaseComponent implements Drawable implements Resizable implements Serializable implements Comparable implements Observable {
	// same-line metadata + a chopped initializer: items continue from the declaration's indent
	@:noCompletion private static var names = [
		"alpha",
		"bravo",
		"charlie",
		"delta",
		"echo",
		"foxtrot",
		"golf",
		"hotel",
		"india",
		"juliet",
		"kilo",
		"lima",
		"mike"
	];

	// empty body: wrapped parameters continue ONE step
	function emptyBodyWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
		fifth:Int):Void {}

	// statements in the body: wrapped parameters continue TWO steps
	function fullBodyWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
			fifth:Int):Void {
		trace(firstParameterName);
	}

	// hand-broken signature that FITS on one line: re-joins
	function joinsAgain(first:Int, second:String, third:Float):Void {
		trace(first);
	}

	// hand-broken signature that does NOT fit: breaks where the joined line reaches the margin
	function breaksAtTheMargin(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool, fifthParameterName:Int,
			sixthParameterName:Int):Void {
		trace(firstParameterName);
	}

	static function main() {
		var checks = new TodoChecks();

		// hand-broken call that fits: re-joins
		checks.joinsAgain(1, "two", 3.0);

		// hand-broken call that does not fit: the last argument moves when `);` would pass the margin
		checks.breaksAtTheMargin(1000000, "a rather long second argument string", 3.0000001, true, 5000000, 600000000);
		checks.breaksAtTheMargin(1000000, "a rather long second argument string that pushes the whole joined line well past the 160 column margin", 3.0000001,
			true, 5000000, 600000000);

		// nested #if inside an inactive branch: fragments and inner directives move as one unit
		#if neverdefined
		if (checks != null) {
			#if debug
			trace("debug");
			#else
			trace("release");
			#end
		}
		#end

		// a comment inside a reformatted element gets the line-comment space
		// inside
		trace(names.length);
	}

	public function new() {}
}
