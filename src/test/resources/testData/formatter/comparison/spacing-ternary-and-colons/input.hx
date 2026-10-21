class Main {
	static var count : Int = 0;

	static function main() {
		var value:Int=count>0?count:0;
		var label : String = value == 0?"none":"some";
		var typed:Float = 1.5;
		trace(label, typed);
	}
}
