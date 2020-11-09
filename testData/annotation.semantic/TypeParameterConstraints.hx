typedef Measurable = {
public var length(default, null):Int;
}

class TypeParameterConstraints {

    public function new() {
         trace(haxe3Syntax([]));
         trace(haxe3Syntax(["bar", "foo"]));

         haxe3Syntax("foo"); // should fail: String should be Iterable<String>
     }

     function haxe3Syntax<T:(Iterable<String>, Measurable)>(a:T) {
         if (a.length == 0) return "empty";
         return a.iterator().next();
     }
     function haxe4Syntax<T:Iterable<String> & Measurable>(a:T) {
         if (a.length == 0) return "empty";
         return a.iterator().next();
     }
     function mixedSyntax<T:(Iterable<String> & Measurable)>(a:T) {
         if (a.length == 0) return "empty";
         return a.iterator().next();
     }

}