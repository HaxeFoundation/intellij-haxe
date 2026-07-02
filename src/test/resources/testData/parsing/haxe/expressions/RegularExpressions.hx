class Regex {
    public function new() {
        var simple = ~/hello/;
        var simpleWithFlags = ~/hello/ig;
        var quantifiers = ~/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/;
        var escapes = ~/^\/usr\/local\/bin\/\w+\.sh$/;
        var groups = ~/(\w+)\s+\1\b/i;
        var NonCapturingGroup = ~/^(?:foo|bar|baz)-\d{1,3}$/m;
        var lookahead = ~/\d+(?=\s*(?:a|b|%))/;
        var negativeLookahead = ~/^(?!\.)[\w.-]+\.(?!exe$)[a-zA-Z0-9]+$/;

        var complex1 = ~/^[\/\\]?(?:[\w.\- ]+[\/\\])*([\w.\- ]+)\.(aa|bb|cc|dd)$/iu;
        var complex2 = ~/^(["'])((?:(?!\1)[^\\]|\\.)*)\1$/gs;
    }
}
