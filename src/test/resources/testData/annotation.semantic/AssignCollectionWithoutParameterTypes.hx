package ;
enum MyNum {
}
class MapTypeParamInitsTest {

	var strings:Array<String>;
	var enums:Array<MyNum>;
	var enumValues:Array<EnumValue>;
	var functions:Array<Int -> Void>;
	var objects:Array<{i:String}>;

	public function testConstrcutors() {
		strings = new Array( );
		enums = new Array();
		enumValues = new Array();
		functions = new Array();
		objects = new Array();
	}

	public function testEmptyCollections() {
		strings = [];
		enums = [];
		enumValues = [];
		functions = [];
		objects = [];

	}
}
