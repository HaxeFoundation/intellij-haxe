package;
class Test {
    function new() {
        var regex = ~/string/i;
        if (regex.match("MyString")) trace("yes");
    }
}