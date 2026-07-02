
package haxe.macro;

enum abstract NullSafetyMode(String) to String {
	/**
		Disable null safety.
	**/
	var Off;

	/**
		Loose safety.
		If an expression is checked `!= null`, then it's considered safe even if it could be modified after the check.
		E.g.
		```haxe
		function example(o:{field:Null<String>}) {
			if(o.field != null) {
				mutate(o);
				var notNullable:String = o.field; //no error
			}
		}

		function mutate(o:{field:Null<String>}) {
			o.field = null;
		}
		```
	**/
	var Loose;

	/**
		Full scale null safety.
		If a field is checked `!= null` it stays safe until a call is made or any field of any object is reassigned,
		because that could potentially alter an object of the checked field.
		E.g.
		```haxe
		function example(o:{field:Null<String>}, b:{o:{field:Null<String>}}) {
			if(o.field != null) {
				var notNullable:String = o.field; //no error
				someCall();
				var notNullable:String = o.field; // Error!
			}
			if(o.field != null) {
				var notNullable:String = o.field; //no error
				b.o = {field:null};
				var notNullable:String = o.field; // Error!
			}
		}
		```
	**/
	var Strict;

	/**
		Full scale null safety for a multi-threaded environment.
		With this mode checking a field `!= null` does not make it safe, because it could be changed from another thread
		at the same time or immediately after the check.
		The only nullable thing could be safe are local variables.
	**/
	var StrictThreaded;
}