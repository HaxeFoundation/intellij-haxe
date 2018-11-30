extern class Array<T> {
	var length(default,null) : Int;
	function new() : Void;
	function concat( a : Array<T> ) : Array<T>;
	function join( sep : String ) : String;
	function pop() : Null<T>;
	function push(x : T) : Int;
	function reverse() : Void;
	function shift() : Null<T>;
	function slice( pos : Int, ?end : Int ) : Array<T>;
	function sort( f : T -> T -> Int ) : Void;
	function splice( pos : Int, len : Int ) : Array<T>;
	function toString() : String;
	function unshift( x : T ) : Void;
	function insert( pos : Int, x : T ) : Void;
	function remove( x : T ) : Bool;
	function copy() : Array<T>;
	function iterator() : Iterator<Null<T>>;
}