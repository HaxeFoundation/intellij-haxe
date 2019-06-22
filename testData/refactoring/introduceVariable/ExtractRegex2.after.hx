package;
class Test {
    function new() {
        var REGEX = ~/string/i;
        if (REGEX.match("MyString")) trace("yes");
    }
}