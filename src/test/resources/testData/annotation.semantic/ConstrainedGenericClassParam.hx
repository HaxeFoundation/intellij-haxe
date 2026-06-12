package ;

class ConstrainedGenericClassParam {

    var items:Array<BaseNode> = [];

    public function new() {}

    public function findByClass<T:BaseNode>(itemClass:Class<T>):Null<T> {
        for (item in items) {
            if (Std.isOfType(item, itemClass)) {
                return cast item;
            }
        }
        return null;
    }

    public function test() {
        // Should NOT report any error - LeafNode is a subtype of BaseNode,
        // and Class<LeafNode> can be assigned to Class<T:BaseNode>
        var specific = findByClass(LeafNode);

        // var with type Int = 0 should not error
        var simpleInt:Int = 0;

        // Std.downcast should accept Class<LeafNode>
        var item:Dynamic = null;
        var downcast = Std.downcast(item, LeafNode);
    }
}

abstract class BaseNode extends RootNode {
    public static inline final TYPE = "";

    function name():String {
        return "base";
    }
}

abstract class RootNode {
    public function new() {}

    abstract function name():String;
}

class LeafNode extends BaseNode {
    public function new() { super(); }

    override function name():String {
        return "leaf";
    }
}
