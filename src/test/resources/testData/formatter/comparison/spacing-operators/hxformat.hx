class Main {
	static function main() {
		var a = 1;
		var b = a + 2;
		var c = b * 3 - a / 2;
		var d = a % 2;
		var e = a << 2 | b >> 1;
		var f = a & 3 ^ b;
		var ok = a < b && b <= c || c != d;
		var same = a == b;
		a += 2;
		b -= 1;
		trace(ok);
	}
}
