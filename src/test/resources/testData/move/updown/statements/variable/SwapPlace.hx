function test() {
    var above = [
        1,
        2,
    ];
    var multiLine<caret> = "
        multi line
        string
        variable
    ";
    var below = [
        1=>"a",
        2=>"b"
    ];
}
