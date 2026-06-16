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

        //valid escape sequences
        "\t";
        "\n";
        "\r";
        "\"";
        "\'";
        '\'';
        "\\";
        "\057";
        "\x5C";
        "\u005C";
        "\u{5C}";
        "\u{005C}";
        "\u{00005C}";

        // invalid escape sequences
        "\T";
        "\957";
        "\xXX";
        "\u{000005C}";
    }

    public static function getVal(s:String) {
      return s;
    }
}