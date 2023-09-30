package ;
class ResolveTypeFromUsage {
    public function new() {

        var x; // inlay should be string

        x = ""; // make variable x get string as type

        var lowerCase = x.toLowerCase(); // should resolve
        var length = lowerCase.length; // should resolve

        x = <error descr="Incompatible type: Int should be String">1</error>; // should fail (expected string)
        var z = <error descr="Unable to apply operator * for types String and Int = 2">x * 2</error> ; // should fail (kan not multiply string and int)
    }
}
