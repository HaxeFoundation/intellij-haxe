package ;
@:meta class TestIssue81 {
    @meta static function main() {
        @meta trace(@meta "Haxe is great!");
        var target = {pos:0}
        var targetpos = @meta @meta @:pos(target){ @:meta 10; }
    }
}