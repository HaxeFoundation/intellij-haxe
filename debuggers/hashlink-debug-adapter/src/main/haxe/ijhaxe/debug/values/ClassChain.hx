package ijhaxe.debug.values;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
	Subtype checks along a class's superclass chain. A class matches a target
	name when the class or one of its superclasses has that full name
	(`pkg.Cls`) or simple name (`Cls`). Interfaces are not checked. Used by
	the `is` operator (ExpressionEvaluator) and by the type filter of
	exception breakpoints.
**/
class ClassChain {
	/**
		True when the class of `type`, or one of its superclasses, has the full or simple name `target`.
	**/
	public static function matches(type:Null<HLType>, target:String):Bool {
		var proto = protoOf(type);
		var seen = 0;
		while (proto != null && seen++ < 64) {
			if (proto.name == target || simpleClassName(proto.name) == target) {
				return true;
			}
			proto = protoOf(proto.tsuper);
		}
		return false;
	}

	static inline function protoOf(type:Null<HLType>):Null<ObjPrototype> {
		return switch (type) {
			case HObj(p), HStruct(p): p;
			default: null;
		}
	}

	public static inline function simpleClassName(full:String):String {
		var dot = full.lastIndexOf(".");
		return dot < 0 ? full : full.substr(dot + 1);
	}
}
