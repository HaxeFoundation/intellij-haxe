package tests.debug.values;

import debug.values.ValuePath;

class ValuePathTest {
	public static function run(assert:Assert):Void {
		parsesPlainIdentifier(assert);
		parsesFieldChain(assert);
		parsesIndexes(assert);
		rejectsNonPaths(assert);
	}

	static function parsesPlainIdentifier(assert:Assert):Void {
		var path = ValuePath.parse("  count ");
		assert.isTrue(path != null, "identifier parses");
		assert.equals("count", path.root, "root name");
		assert.equals(0, path.accessors.length, "no accessors");
	}

	static function parsesFieldChain(assert:Assert):Void {
		var path = ValuePath.parse("obj.inner.value");
		assert.equals("obj", path.root, "chain root");
		assert.equals(2, path.accessors.length, "two field accessors");
		assert.isTrue(path.accessors[0].match(Field("inner")), "first accessor");
		assert.isTrue(path.accessors[1].match(Field("value")), "second accessor");
	}

	static function parsesIndexes(assert:Assert):Void {
		var path = ValuePath.parse("items[3].name");
		assert.isTrue(path.accessors[0].match(Index(3)), "index accessor");
		assert.isTrue(path.accessors[1].match(Field("name")), "field after index");
	}

	static function rejectsNonPaths(assert:Assert):Void {
		assert.isTrue(ValuePath.parse("n + 1") == null, "arithmetic rejected");
		assert.isTrue(ValuePath.parse("f()") == null, "calls rejected");
		assert.isTrue(ValuePath.parse("a[b]") == null, "non-numeric index rejected");
		assert.isTrue(ValuePath.parse("a.") == null, "trailing dot rejected");
		assert.isTrue(ValuePath.parse("1abc") == null, "leading digit rejected");
		assert.isTrue(ValuePath.parse("") == null, "empty rejected");
		assert.isTrue(ValuePath.parse(null) == null, "null rejected");
	}
}
