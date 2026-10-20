class Main {
	var rand:Void->Int;
	var fold:Int->String->Void;
	var apply:(Int->Int)->Int;
	var nop:() -> Void;
	var one:(Int) -> Void;
	var two:(Int, String) -> Void;

	static function main() {
		var twice:(value:Int) -> Int = value -> value * 2;
		var join:(a:String, b:String) -> String = (a, b) -> a + b;
		var pick:Int->(Int->Int) = i -> j -> i + j;
		trace(twice(2), join("x", "y"), pick(1)(2));
	}
}
