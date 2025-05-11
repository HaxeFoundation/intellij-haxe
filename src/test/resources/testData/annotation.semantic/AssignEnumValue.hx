package;

class AssignEnumValue {
	public function new() {
		var array : Array<MyEnum>;
		var ok1:EnumValue = MyEnum.FIRST;
		var ok2:EnumValue = enuVal;

		var tmp:MyEnum = MyEnum.SECOND;
		var ok3:EnumValue = tmp;
		var ok4:EnumValue = getEnum();
		var ok5:EnumValue = generic(array);

		var wrong1:EnumValue = <error descr="Incompatible type: Enum<MyEnum> should be EnumValue">MyEnum</error>;
		// expression is read it as  x < y, so it thinks its a bool expression (not sure if we want this error or not)
		var wrong2:EnumValue = <error descr="Unable to apply operator < for types Class<Enum> and Enum<MyEnum>">Enum<MyEnum</error>><error descr="<expression> expected, got ';'">;</error>

			// empty constrctor tests
		var ok6:EnumValue = MyEnum.EMPTY_CONSTRUCTOR; // this is the correct way to use "EMPTY_CONSTRUCTOR()"
			//TODO mlo:this should show an error (Error: MyEnum cannot be called)
		var wrong3:EnumValue = MyEnum.EMPTY_CONSTRUCTOR(); // while this might seem logical it fails to compile

	}

	public function getEnum():MyEnum {
		return MyEnum.THIRD;
	}
	function generic<T:EnumValue>(arr:Array<T>):T {
		return err.get(0);
	}
}
enum MyEnum {
	FIRST;
	SECOND;
	THIRD;
	EMPTY_CONSTRUCTOR();
}
