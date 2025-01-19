package ;
enum MyNum {
}
class MapTypeParamInitsTestArray {

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

class MapTypeParamInitsTestMap{
	var strings:Map<String, String>;
	var enums:Map<String, MyNum>;
	var enumValues:Map<String, EnumValue>;
	var functions:Map<String, Int -> Void>;
	var objects:Map<String, {i:String}>;

	public function testConstrcutors() {
		strings = new Map( );
		enums = new Map();
		enumValues = new Map();
		functions = new Map();
		objects = new Map();
	}

	public function testEmptyCollections() {
		strings = [];
		enums = [];
		enumValues = [];
		functions = [];
		objects = [];
	}
}