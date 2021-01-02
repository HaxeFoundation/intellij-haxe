import haxe.ds.Vector;
class ParenthesizedExpression {
    function test(){
        var tmp = Vector.fromArrayCopy(["A","B"]);
        tmp.get(0).<caret>;
    }
}