class Main {
	static function main() {
		var n = 2;
		var s = 'count ${ n+1 } and ${n * 2} plus $n end';
		var t = 'sum ${ n + n*3 }!';
		trace(s + t);
	}
}
