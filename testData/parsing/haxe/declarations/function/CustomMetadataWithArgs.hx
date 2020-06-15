class EmptyCustomMacro {
    @CustomMacro("arg1", 2, 54.3, {next:()->0})
    @CustomMacro(arg1, arg2, arg3)  // Should be an error on this line.
    public function new() {
    }
}