package;

class MethodSignatureWrap {
	private function declaredWrapped(firstValue:Array<Int>, secondValue:Array<Int>, thirdValue:String, fourthValue:Float, fifthValue:Int, sixthValue:Bool,
seventhValue:Int):Array<Int> {
		return firstValue;
	}

	@:keep private static function withMeta(firstValue:Array<Int>, secondValue:Array<Int>, thirdValue:String, fourthValue:Float, fifthValue:Int,
							sixthValue:Bool, seventhValue:Int):Array<Int> {
		return firstValue;
	}

	public function new(firstValue:Array<Int>, secondValue:Array<Int>, thirdValue:String, fourthValue:Float, fifthValue:Int, sixthValue:Bool,
	seventhValue:Int, eighthValue:Int) {
		trace(firstValue);
	}
}
