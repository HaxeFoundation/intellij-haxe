extern class String {
	static var __is_String;
	private static var __split : Dynamic;
	static function __init__() : Void;
	public var length(default,null) : Int;
	public function new(s:String) : Void;
	public function charAt(index:Int) : String;
	public function charCodeAt(index : Int) : Null<Int>;
	public function indexOf( str : String, ?startIndex : Int ) : Int;
	public function lastIndexOf( str : String, ?startIndex : Int ) : Int;
	public function split( delimiter : String ) : Array<String>;
	public function substr( pos : Int, ?len : Int ) : String;
	public function toLowerCase() : String;
	public function toUpperCase() : String;
	public function toString() : String;
}
