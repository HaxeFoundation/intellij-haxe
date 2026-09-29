class Main {
	static function parse(pack:Array<String>) {
		return switch stream {
			case [{tok:Const(CIdent(i)), pos:p}]: { name: i, pos: p };
			case [{tok:Kwd(KwdMacro), pos:p} && pack.length > 0]: { name: "macro", pos: p };
			case [{tok:Const(CIdent(i))} && isLower(i)]: i;
			case [x = ident(), {tok:Comma} && true]: x;
		}
	}
}
