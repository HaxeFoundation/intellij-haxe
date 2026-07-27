package tests;

import ijhaxe.hxcpp.debug.values.VariablesView;

class VariablesViewTest {
	public static function run(assert:Assert):Void {
		frameLocalsListWithValues(assert);
		expandableValuesGetAChildReference(assert);
		setVariableWritesToTheFrame(assert);
		resetInvalidatesReferences(assert);
	}

	static function make():{view:VariablesView, api:FakeDebuggerApi} {
		var api = new FakeDebuggerApi();
		return {view: new VariablesView(api), api: api};
	}

	static function frameLocalsListWithValues(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["count", "name"];
		t.api.localValues.set("count", 7);
		t.api.localValues.set("name", "bob");
		var ref = t.view.frameScope(1, 0);
		var vars = t.view.variables(ref);
		assert.equals(2, vars.length, "two locals");
		assert.equals("count", vars[0].name, "local name");
		assert.equals("7", vars[0].value, "local value");
		assert.equals(0, vars[0].variablesReference, "primitive has no children");
	}

	static function expandableValuesGetAChildReference(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["nums"];
		t.api.localValues.set("nums", [1, 2, 3]);
		var vars = t.view.variables(t.view.frameScope(1, 0));
		var childRef = vars[0].variablesReference;
		assert.isTrue(childRef > 0, "array local is expandable");
		var elements = t.view.variables(childRef);
		assert.equals(3, elements.length, "three elements");
		assert.equals("[2]", elements[2].name, "indexed child");
		assert.equals("3", elements[2].value, "child value");
	}

	static function setVariableWritesToTheFrame(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 7);
		var ref = t.view.frameScope(2, 1); // thread 2, frame 1 (ANY frame)
		var result = t.view.setVariable(ref, "count", "42");
		assert.equals("42", result.value, "new value rendered");
		assert.equals(1, t.api.setVarCalls.length, "write reached the runtime");
		assert.equals(2, t.api.setVarCalls[0].thread, "on the right thread");
		assert.equals(1, t.api.setVarCalls[0].frame, "on the right (non-top) frame");
		assert.equals(42, t.api.setVarCalls[0].value, "parsed to an Int");
	}

	static function resetInvalidatesReferences(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["x"];
		t.api.localValues.set("x", 1);
		var ref = t.view.frameScope(1, 0);
		assert.equals(1, t.view.variables(ref).length, "valid before reset");
		t.view.reset();
		assert.equals(0, t.view.variables(ref).length, "stale reference yields nothing after reset");
	}
}
