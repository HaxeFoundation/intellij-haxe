/**
	Interface / virtual fixture: a class that EXTENDS a base and IMPLEMENTS an
	interface, so the object carries genhl's hidden per-interface virtual cache
	field (empty name, HVirtual type) beside its own fields, and an
	interface-typed local holds an object-backed vvirtual.

	The interface deliberately mixes the three member kinds a virtual can hold:
	a REAL field (count), an accessor-backed property (label) and a method
	(describe) — only the real field maps to a physical slot in the object.

	WARNING: line numbers are load-bearing test constants
	(FIXTURE_IFACE_LINE in DapIntegrationTestBase) — update them together.
**/
interface ITask {
	var count:Int;
	var label(get, never):String;
	function describe():String;
}

class TaskBase {
	public var baseId:Int;

	public function new(id:Int) {
		baseId = id;
	}
}

class AnimTask extends TaskBase implements ITask {
	public var count:Int;
	public var name:String;

	public function new(n:Int) {
		super(n * 2);
		count = n;
		name = "anim" + n;
	}

	public var label(get, never):String;

	public function get_label():String {
		return "L" + name;
	}

	public function describe():String {
		return name + ":" + count;
	}
}

class Iface {
	public static function demo():Void {
		var n = Std.parseInt("4");
		var task = new AnimTask(n); // the object itself (carries the hidden cache field)
		var asIface:ITask = task; // an object-backed virtual
		Sys.println(asIface.describe() + task.baseId); // FIXTURE_IFACE_LINE = 54
	}
}
