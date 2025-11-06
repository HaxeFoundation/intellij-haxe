class TestClass {
    function main() {
        if (true)
        {
        }
        else if (false)
        {
            var multiLine<caret> =
            [
                "multi line",
                "string",
                "variable"
            ];
        }
        else
        {
        }
    }
}