interface Renderer {
	function drawEverythingWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
		fifth:Int):Void;
}

extern class NativeBridge {
	static function bindEverythingWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
		fifth:Int):Void;
}

class Main {
	function emptyBodyWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
		fifth:Int):Void {}

	function fullBodyWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
			fifth:Int):Void {
		trace(firstParameterName);
	}

	function expressionBodyWithTheseParameters(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
			fifth:Int):Int
		return fifth;

	function overflowingFirstParameterMovesTheSecond(aVeryLongFirstParameterNameThatReachesTheMarginAllByItselfBecauseItIsReallyReallyLong:Int,
			secondParameterName:String, third:Float):Void {
		trace(third);
	}

	static function main() {
		var callback = function(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
			fifthParameterName:Int):Void {};
		var worker = function(firstParameterName:Int, secondParameterName:String, thirdParameterName:Float, fourthParameterName:Bool,
				fifthParameterName:Int):Void {
			trace(fifthParameterName);
		};
	}
}
