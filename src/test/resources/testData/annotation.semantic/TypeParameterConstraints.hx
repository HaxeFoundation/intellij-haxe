typedef Measurable = {
public var length(default, null):Int;
}

class TypeParameterConstraints {

    public function new() {
         trace(haxe3Syntax([]));
         trace(haxe3Syntax(["bar", "foo"]));

        haxe3Syntax(<error descr="Type mismatch (Expected: '(Iterable<String>, Measurable)' got: 'String')">"foo"</error>); // should fail: String should be Iterable<String>
        haxe3Syntax(<error descr="Type mismatch (Expected: '(Iterable<String>, Measurable)' got: 'Array<Int>')">[1,  1]</error>); // should fail, wrong type parameter (got int expects string)
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