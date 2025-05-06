class SafeCastExpressions {
    public function new() {
        var object:Dynamic;
        var a =   cast(object, String).toLowerCase().length;
        var b = (cast(object, String)).toLowerCase().length;

        // This type parameter cast is not allowed, but it shows resolve
        var c = cast(object, Map<String, String>)[0].charAt(0).toLowerCase().length;
        var d = (cast(object, Map<String, String>))[0].charAt(0).toLowerCase().length;

        //casting to functionTypes is also not allowed but this is here to show resolve
        var d = cast(object, Map<String, String->String>)[0]("dsd" ).charAt(0).toLowerCase().length;
        var d = (cast(object, Map<String, String->String>))[0]("dsd").charAt(0).toLowerCase().length;
    }
}
