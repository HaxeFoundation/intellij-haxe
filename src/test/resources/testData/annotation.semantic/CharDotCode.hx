package;
class CharDotCode {
    public function new() {
        var x = "A".code;
        var x = "\t".code;
        var x = "\n".code;
        var x = "\r".code;
        var x = "\"".code;
        var x = "\'".code;
        var x = '\''.code;
        var x = "\\".code;
        var x = "\057".code;
        var x = "\x5C".code;
        var x = "\u005C".code;
        var x = "\u{5C}".code;
        var x = "\u{005C}".code;
        var x = "\u{00005C}".code;


        // Wrong
        var x = "".<warning descr="Unresolved symbol">code</warning>; // NOT single char
        var x = "AB".<warning descr="Unresolved symbol">code</warning>; // NOT single char
        var x = "<error descr="Illegal escape character in string literal">\9</error>57".<warning descr="Unresolved symbol">code</warning>; // NOT octal
        var x = "<error descr="Illegal escape character in string literal">\x</error>XX".code; // NOT  hex values
        var x = "<error descr="Illegal escape character in string literal">\u</error>{000005C}".<warning descr="Unresolved symbol">code</warning> ; // Too many hex digits (but seems to work in JS)
    }
}
