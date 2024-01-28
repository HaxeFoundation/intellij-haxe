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

 		var <error descr="Incompatible type: Enum<MyEnum> should be EnumValue">wrong1:EnumValue = MyEnum</error>;
		// expression is read it as  x < y, so it thinks its a bool expression (not sure if we want this error or not)
 		var wrong2:EnumValue = <error descr="Unable to apply operator < for types Enum<unknown> and MyEnum">Enum<MyEnum</error>><error descr="<expression> expected, got ';'">;</error>
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
}
