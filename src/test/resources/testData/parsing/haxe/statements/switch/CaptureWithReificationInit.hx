class Main {
	static function parse(el:Array<Expr>) {
		return switch stream {
			case [e = macro $b{el}]: e;
			case [e = macro $v{el.length}, {tok:Comma}]: e;
		}
	}
}
