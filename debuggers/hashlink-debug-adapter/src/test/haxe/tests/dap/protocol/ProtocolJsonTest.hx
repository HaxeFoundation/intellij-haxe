package tests.dap.protocol;

import ijhaxe.dap.protocol.Response;
import haxe.Json;

class ProtocolJsonTest {
	public static function run(assert:Assert):Void {
		optionalFieldsAreOmittedWhenAbsent(assert);
		responseSerializesAllAssignedFields(assert);
	}

	static function optionalFieldsAreOmittedWhenAbsent(assert:Assert):Void {
		var response:Response = {
			seq: 1,
			type: "response",
			request_seq: 1,
			success: true,
			command: "configurationDone"
		};
		var parsed:Dynamic = Json.parse(Json.stringify(response));
		assert.isFalse(Reflect.hasField(parsed, "body"), "unassigned body omitted from JSON");
		assert.isFalse(Reflect.hasField(parsed, "message"), "unassigned message omitted from JSON");
	}

	static function responseSerializesAllAssignedFields(assert:Assert):Void {
		var response:Response = {
			seq: 3,
			type: "response",
			request_seq: 2,
			success: false,
			command: "foo",
			message: "Unrecognized command: foo"
		};
		response.body = {error: {id: 1000, format: "Unrecognized command: foo", showUser: false}};

		var parsed:Dynamic = Json.parse(Json.stringify(response));
		assert.equals(3, parsed.seq, "seq");
		assert.equals("response", parsed.type, "type");
		assert.equals(2, parsed.request_seq, "request_seq");
		assert.equals(false, parsed.success, "success");
		assert.equals("foo", parsed.command, "command");
		assert.equals("Unrecognized command: foo", parsed.message, "message");
		assert.equals(1000, parsed.body.error.id, "body.error.id");
	}
}
