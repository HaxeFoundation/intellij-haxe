package debug.values;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
 * Subtype matching over the HL object super-chain: an object's runtime class
 * "is" a target type when the target equals the class or any of its
 * superclasses, compared by full name (`pkg.Cls`) or simple name (`Cls`).
 * Interfaces are not part of the chain. Pure and stateless — shared by the `is`
 * operator (ExpressionEvaluator) and type-filtered exception breakpoints.
 */
class ClassChain {
	/** True when `type`'s class or a superclass matches `target` (full or simple name). */
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
