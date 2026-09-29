class Main {
	static function main() {
		var n:Any = 5;
		var a = (n:Int);
		var b = ( n  :  Int );
		var c = ((n:Int));
		var d = (cast n:Int);
		trace(a + b + c + d);
	}
}
