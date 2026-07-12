package tests;

import debug.Pointer;
import debug.session.Breakpoints;

import haxe.Int64;

class BreakpointsTest {
	static inline var INT3 = 0xCC;

	public static function run(assert:Assert):Void {
		installsInt3AndSavesOriginalByte(assert);
		replacingSourceRestoresOldAndInstallsNew(assert);
		suspendAndRearmToggleTheByte(assert);
		duplicateAddressReusesBreakpoint(assert);
		tempBreakpointsPatchAndRestore(assert);
		tempSharedWithUserBreakpointNotRestored(assert);
	}

	static function addr(v:Int):Pointer {
		return Int64.add(Int64.ofInt(0x400000), Int64.ofInt(v));
	}

	static function loc(a:Int, line:Int, id:Int = 1):{id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int, condition:Null<String>} {
		return {id: id, address: addr(a), fidx: 0, op: 0, file: "Main.hx", line: line, condition: null};
	}

	static function installsInt3AndSavesOriginalByte(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(10), 0x55); // some original instruction byte
		var bps = new Breakpoints(api, 1234);

		var installed = bps.setForSource("Main.hx", [loc(10, 14)]);
		assert.equals(1, installed.length, "one breakpoint installed");
		assert.equals(0x55, installed[0].originalByte, "original byte saved");
		assert.equals(14, installed[0].line, "line recorded");
		assert.equals(INT3, api.peek(addr(10)), "INT3 written to memory");
		assert.isTrue(api.flushed > 0, "instruction cache flushed");
		assert.isTrue(bps.isBreakpointAddress(addr(10)), "address is a known breakpoint");
		assert.isTrue(bps.atAddress(addr(10)) != null, "lookup by address");
	}

	static function replacingSourceRestoresOldAndInstallsNew(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(10), 0x55);
		api.poke(addr(20), 0x66);
		var bps = new Breakpoints(api, 1);

		bps.setForSource("Main.hx", [loc(10, 14)]);
		var second = bps.setForSource("Main.hx", [loc(20, 20)]);

		assert.equals(0x55, api.peek(addr(10)), "old breakpoint byte restored");
		assert.equals(INT3, api.peek(addr(20)), "new breakpoint installed");
		assert.isFalse(bps.isBreakpointAddress(addr(10)), "old address no longer tracked");
		assert.isTrue(bps.isBreakpointAddress(addr(20)), "new address tracked");
		assert.isTrue(second[0].id != 0, "new breakpoint has an id");
	}

	static function suspendAndRearmToggleTheByte(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(30), 0x90);
		var bps = new Breakpoints(api, 1);
		var bp = bps.setForSource("Main.hx", [loc(30, 14)])[0];

		assert.equals(INT3, api.peek(addr(30)), "installed as INT3");
		bps.suspend(bp);
		assert.equals(0x90, api.peek(addr(30)), "suspend restores original byte");
		bps.rearm(bp);
		assert.equals(INT3, api.peek(addr(30)), "rearm restores INT3");
	}

	static function tempBreakpointsPatchAndRestore(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(50), 0x42);
		api.poke(addr(60), 0x43);
		var bps = new Breakpoints(api, 1);

		assert.isFalse(bps.hasTemps(), "no temps initially");
		bps.addTemp(addr(50));
		bps.addTemp(addr(60));
		assert.equals(INT3, api.peek(addr(50)), "temp installs INT3");
		assert.equals(INT3, api.peek(addr(60)), "second temp installs INT3");
		assert.isTrue(bps.isTemp(addr(50)), "address reported as temp");
		assert.isTrue(bps.hasTemps(), "hasTemps true after add");

		bps.addTemp(addr(50)); // idempotent
		bps.clearTemps();
		assert.equals(0x42, api.peek(addr(50)), "clearTemps restores original byte");
		assert.equals(0x43, api.peek(addr(60)), "clearTemps restores second byte");
		assert.isFalse(bps.hasTemps(), "no temps after clear");
	}

	static function tempSharedWithUserBreakpointNotRestored(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(70), 0x55);
		var bps = new Breakpoints(api, 1);
		bps.setForSource("Main.hx", [loc(70, 14)]); // user bp -> INT3 at addr 70

		bps.addTemp(addr(70)); // shares the user breakpoint's address
		assert.equals(INT3, api.peek(addr(70)), "still INT3 while temp shares it");
		bps.clearTemps();
		// clearing the temp must NOT restore the byte the user breakpoint owns
		assert.equals(INT3, api.peek(addr(70)), "user breakpoint survives clearTemps");
		assert.isTrue(bps.isBreakpointAddress(addr(70)), "user breakpoint still tracked");
	}

	static function duplicateAddressReusesBreakpoint(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.poke(addr(40), 0x11);
		var bps = new Breakpoints(api, 1);
		// two source entries resolving to the same address (e.g. inlined line)
		var installed = bps.setForSource("Main.hx", [loc(40, 14), loc(40, 14)]);
		assert.equals(2, installed.length, "both entries returned");
		assert.equals(installed[0].id, installed[1].id, "same address reuses one breakpoint");
		assert.equals(0x11, installed[0].originalByte, "original byte not overwritten by INT3 re-read");
	}
}
