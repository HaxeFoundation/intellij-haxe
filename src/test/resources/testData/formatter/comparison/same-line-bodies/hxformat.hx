class Main {
	static function main() {
		var x = 3;
		if (x > 0)
			trace("positive");
		if (x > 1)
			trace("big");
		else
			trace("small");
		for (i in 0...3)
			trace(i);
		while (x > 0)
			x--;
		try
			trace(x)
		catch (e:Dynamic)
			trace(e);
		try {
			x++;
		} catch (e:Dynamic)
			trace("caught");
		trace(x);
	}
}
