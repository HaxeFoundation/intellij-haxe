class Main {
	static function parse(pack:Array<String>, limit:Int) {
		return switch stream {
			case [{tok:Const(CIdent(i)), pos:p} && pack.length > 0 || limit == 0]: { name: i, pos: p };
			case [{tok:Kwd(KwdMacro), pos:p} == null]: p;
			case [{tok:Comma} && limit > 1, {tok:Semicolon} + limit < 3]: limit;
		}
	}
	static function classify(value:Option<Int>, limit:Int) {
		return switch value {
			case Some(x) && x > limit: x;
			case {tok:Comma, pos:p} || limit == 0: p;
			case _: 0;
		}
	}
}
