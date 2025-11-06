class TestClass {
    function main() {
        if (true)
        {
            var multiLine<caret> =
            [
                "multi line",
                "string",
                "variable"
            ];
        }
    }
}