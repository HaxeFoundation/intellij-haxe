private typedef NormalizedUVT = {
	max:Float,
	uvt:Vector<Float>
}

typedef Extended = {
	> NormalizedUVT,
	name:String
}

typedef Inline = {a:Int, b:Int};

class A {
	var point:{x:Int, y:Int};
	var multi:{
		x:Int,
		y:Int
	};
	function f(p:{x:Int, y:Int}):{x:Int, y:Int} {
		return p;
	}
}
