class TestClass {
    function main() {
        try
        {
            var multiLine<caret> =
            [
                "multi line",
                "string",
                "variable"
            ];
        }
        catch (firstBlock)
        {
        }
    }
}