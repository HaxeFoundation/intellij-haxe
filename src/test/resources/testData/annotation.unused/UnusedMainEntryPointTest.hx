class UnusedMainEntryPointTest {
    // the build starts here, so the compiler calls it
    static function main() {}
}

class NotAnEntryPoint {
    static function <warning descr="Method 'main' is never used">main</warning>() {}
}
