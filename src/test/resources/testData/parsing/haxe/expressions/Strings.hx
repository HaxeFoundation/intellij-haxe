package;
class Test {
    public static function main() {
        var foo = 1;
        "".charAt(0);
        "$foo";
        "$$foo";
        "absdf$foo";
        "$$${foo";
        '$$$foo';
        "ab${trace('')}";
        "$$${trace()}";
        "$$${trace(";")}";
        '$$${getVal("'")}\'';
        'ca$$h money'; // Issue 303.
    }

    public static function getVal(s:String) {
      return s;
    }
}