package;
class Test {
    public static function main() {
        var foo = 1;
        "".charAt(0);
        <warning descr="An expression that contains string interpolation should be wrapped with single-quotes.">"$foo"</warning>;
        "$$foo";
        <warning descr="An expression that contains string interpolation should be wrapped with single-quotes.">"absdf$foo"</warning>;
        "$$${foo";
        '$$$foo';
        <warning descr="An expression that contains string interpolation should be wrapped with single-quotes.">"ab${trace('')}"</warning>;
        <warning descr="An expression that contains string interpolation should be wrapped with single-quotes.">"$$${trace()}"</warning>;
        "$$${trace(";")}";
        '$$${getVal("'")}\'';
        'ca$$h money'; // Issue 303.
    }

    public static function getVal(s:String) {
      return s;
    }
}