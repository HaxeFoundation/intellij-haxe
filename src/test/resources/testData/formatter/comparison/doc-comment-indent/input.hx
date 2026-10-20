class Foo {
	/**
		Cancels a specified `setInterval()` call.

		@param	id	The ID of the call, set to a variable, as
in the following:

		```haxe
			var id = setInterval(func, 1000);
		```
	**/
	public static function clearInterval(id:UInt):Void {
		trace(id);
	}

	/**
	 * Starred style with a tag.
	 * @param x the value
	 */
	function starred(x:Int):Void {
		trace(x);
	}
}

class Scope {
/**
	zero-column doc
**/
	function a():Void {
		trace(1);
	}

			/**
				over-indented doc
			**/
	function b():Void {
		trace(2);
	}

/* zero-column plain comment */
	function c():Void {
		trace(3);
	}
}
