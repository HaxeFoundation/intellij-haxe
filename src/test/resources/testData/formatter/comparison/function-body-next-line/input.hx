class Main {
	static function shout() trace("loud");

	static function twice(v:Int):Int return v * 2;

	static function fail() throw "nope";

	static function main() {
		function local(v:Int):Int return v + 10;
		var anon = function(v:Int) return v + 1;
		var arrow = (v:Int) -> v + 1;
		trace(local(twice(1)) + anon(2) + arrow(3));
		shout();
	}
}
