package ;

// @:autoBuild on a parent class. The macro adds static helpers
// (register, describe, KIND) to every subclass; the plugin can't run the macro,
// so it cannot see those members. It must NOT flag their usage as
// "Unresolved symbol".
@:autoBuild(<warning descr="Unresolved symbol">AutoBuildMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class BuiltBase {
}

class ChildOfBuilt extends BuiltBase {
}

// @:build directly on the class — same rule applies, since the macro
// can inject members straight into this class.
@:build(<warning descr="Unresolved symbol">DirectBuildMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class DirectlyBuilt {
}

// No build/autoBuild anywhere; missing static members must still be flagged.
class Plain {
}

class Caller {
    static function main() {
        // Macro-injected via @:autoBuild on the parent.
        ChildOfBuilt.register(5);
        ChildOfBuilt.describe();
        var t = ChildOfBuilt.KIND;

        // Macro-injected via @:build on the class itself.
        DirectlyBuilt.synthesised();

        // No macro on Plain's hierarchy — must still flag.
        Plain.<warning descr="Unresolved symbol">missing</warning>();
    }
}
