class TestClass {
    // NOTE: the strange code format is intentional to verify that we move the correct line ranges
    function main() {
        do
        {
            var multiLine<caret> =
            [
                "multi line",
                "string",
                "variable"
            ];
        }
        while
        (true);
    }
}