class Main {
	var randomSeed = 1;

	var seeded:Void->Int = {
		function func():Int {
			return randomSeed;
		}
	};

	static function main() {
		var rand:Void->Int = {
			function func():Int {
				randomSeed = randomSeed * 1103515245 + 12345;
				return Std.int(Math.abs(randomSeed / 65536)) % 32768;
			}
		};
		var lambda = function(x:Int):Int {
			return x + 1;
		};
		var arrow = (x:Int) -> {
			return x + 2;
		};
		var point = {
			x: 10,
			y: 20
		};
		trace(rand(), lambda(1), arrow(2), point.x);
	}
}
