package ;

// @:autoBuild on a parent class. The macro adds static helpers
// (event, map, EVENT_TYPE) to every subclass; the plugin can't run the macro,
// so it cannot see those members. It must NOT flag their usage as
// "Unresolved symbol".
@:autoBuild(<warning descr="Unresolved symbol">EzCommandMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class EzCommand {
}

class PickupProductionCommand extends EzCommand {
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
        PickupProductionCommand.event(5);
        PickupProductionCommand.map();
        var t = PickupProductionCommand.EVENT_TYPE;

        // Macro-injected via @:build on the class itself.
        DirectlyBuilt.synthesised();

        // No macro on Plain's hierarchy — must still flag.
        Plain.<warning descr="Unresolved symbol">missing</warning>();
    }
}
