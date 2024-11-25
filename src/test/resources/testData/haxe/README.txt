There are some important changes between 4.x versions of haxe that results in different behavior.
One such change is that the Any type changes from using implicit from cast, to using explicit from cast.
one effect this has is that typeParameters of other types  can now be assigned to Any.
This means that our tests will behave differently depending on version used.

ex.
var x:Array<Any>  = null;
var y:Array<String>  = null;

// not allowed in 4.1.5,
// but allowed since 4.2.5
x = y;