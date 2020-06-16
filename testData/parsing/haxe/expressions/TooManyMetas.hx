package ;
@:meta class TestIssue81 {
    @meta static function main() {
        @meta trace(@meta "Haxe is great!");
        var target = /** @anon( "anonData" ) */ {pos:0}
        var @meta targetpos = @meta @meta @:pos(target){ @:meta 10; }
    }
}