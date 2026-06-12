class UnusedMethodPostConstructTest {
    public function new() {}

    //  invoked reflectively by DI frameworks after construction
    @:postConstruct
    function compileTimeMetaInit() {}

    @postConstruct
    function runTimeMetaInit() {}

    function <warning descr="Method 'plainUnusedMethod' is never used">plainUnusedMethod</warning>() {}
}
