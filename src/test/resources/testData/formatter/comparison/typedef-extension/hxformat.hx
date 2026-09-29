typedef Base = {
	var id:Int;
}

typedef Extended = {
	> Base,
	var name:String;
}

class Main {
	static function main() {
		var s:{ > Base, var extra:Int;} = null;
		trace(s);
	}
}
