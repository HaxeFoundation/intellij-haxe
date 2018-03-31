typedef Point = {
    var x : Int;
    var y : Int;
}

class Points{
    function foo(){
        var p : {> Point, z : Int }
        p = { x : 0, y : 0, z : 0 };
    }
}