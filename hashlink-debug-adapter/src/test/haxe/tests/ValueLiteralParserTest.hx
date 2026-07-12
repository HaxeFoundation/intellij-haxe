package tests;

import debug.values.ValueLiteral;
import debug.values.ValueLiteralParser;

import haxe.Int64;

class ValueLiteralParserTest {
	public static function run(assert:Assert):Void {
		parsesIntegers(assert);
		parsesFloatsBoolsNull(assert);
		parsesHexAndNegatives(assert);
		parsesPathsAndRejectsGarbage(assert);
	}

	static function parsesIntegers(assert:Assert):Void {
		assert.isTrue(match(ValueLiteralParser.parse("42"), LInt(Int64.ofInt(42))), "decimal int");
		assert.isTrue(match(ValueLiteralParser.parse("  7 "), LInt(Int64.ofInt(7))), "trimmed int");
	}

	static function parsesFloatsBoolsNull(assert:Assert):Void {
		assert.isTrue(ValueLiteralParser.parse("3.5").match(LFloat(3.5)), "float");
		assert.isTrue(ValueLiteralParser.parse("true").match(LBool(true)), "true");
		assert.isTrue(ValueLiteralParser.parse("false").match(LBool(false)), "false");
		assert.isTrue(ValueLiteralParser.parse("null").match(LNull), "null");
	}

	static function parsesHexAndNegatives(assert:Assert):Void {
		assert.isTrue(match(ValueLiteralParser.parse("0xFF"), LInt(Int64.ofInt(255))), "hex");
		assert.isTrue(match(ValueLiteralParser.parse("-5"), LInt(Int64.ofInt(-5))), "negative int");
		assert.isTrue(match(ValueLiteralParser.parse("-0x10"), LInt(Int64.ofInt(-16))), "negative hex");
	}

	static function parsesPathsAndRejectsGarbage(assert:Assert):Void {
		assert.isTrue(isPath(ValueLiteralParser.parse("other")), "bare identifier is a copy path");
		assert.isTrue(isPath(ValueLiteralParser.parse("obj.field")), "field path");
		assert.isTrue(ValueLiteralParser.parse("") == null, "empty rejected");
		assert.isTrue(ValueLiteralParser.parse("1 + 2") == null, "expression rejected");
		assert.isTrue(ValueLiteralParser.parse("-name") == null, "negated identifier rejected");
	}

	static function match(literal:Null<ValueLiteral>, expected:ValueLiteral):Bool {
		return switch [literal, expected] {
			case [LInt(a), LInt(b)]: Int64.compare(a, b) == 0;
			default: false;
		}
	}

	static function isPath(literal:Null<ValueLiteral>):Bool {
		return literal != null && literal.match(LPath(_));
	}
}
