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


        // BAD
        var x = "".<warning descr="Unresolved symbol">code</warning>; // NOT single char
        var x = "AB".<warning descr="Unresolved symbol">code</warning>; // NOT single char
        var x = "\957".<warning descr="Unresolved symbol">code</warning>; // NOT octal
        //TODO mlo  make sure incorrect  values are handled as expected
        var x = "\xXX".code; // NOT  hex values
        var x = "\u{000005C}".<warning descr="Unresolved symbol">code</warning> ; // Too many hex digits (but seems to work in JS)
    }
}
