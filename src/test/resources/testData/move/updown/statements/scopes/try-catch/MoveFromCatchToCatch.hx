class TestClass {
    function main() {
        try
        {
        }
        catch (firstBlock)
        {
        }
        catch (secondBlock)
        {
            var multiLine<caret> =
            [
                "multi line",
                "string",
                "variable"
            ];
        }
        catch (thirdBlock)
        {
        }
    }
}