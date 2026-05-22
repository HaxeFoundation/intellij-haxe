package ;

class ConstrainedGenericClassParam {

    var items:Array<AbstractBaseVO> = [];

    public function new() {}

    public function findByClass<T:AbstractBaseVO>(itemClass:Class<T>):Null<T> {
        for (item in items) {
            if (Std.isOfType(item, itemClass)) {
                return cast item;
            }
        }
        return null;
    }

    public function test() {
        // Should NOT report any error - SpecificVO is a subtype of AbstractBaseVO,
        // and Class<SpecificVO> can be assigned to Class<T:AbstractBaseVO>
        var specific = findByClass(SpecificVO);

        // var with type Int = 0 should not error
        var simpleInt:Int = 0;

        // Std.downcast should accept Class<SpecificVO>
        var item:Dynamic = null;
        var downcast = Std.downcast(item, SpecificVO);
    }
}

abstract class AbstractBaseVO extends AbstractVO {
    public static inline final TYPE = "";

    function _getClassName():String {
        return "AbstractBase";
    }
}

abstract class AbstractVO {
    public function new() {}

    abstract function _getClassName():String;

    public function toJSONObject():Dynamic {
        return {__class__: _getClassName()};
    }

    public function fromJSONObject(raw:Dynamic):Void {}
}

class SpecificVO extends AbstractBaseVO {
    public function new() { super(); }

    override function _getClassName():String {
        return "Specific";
    }
}
