package;

class AssignEnumValue {
 	public function new() {

 		var ok1:EnumValue = MyEnum.FIRST;
 		var ok2:EnumValue <warning descr="Unable to determine type, assignment might be incorrect">= enuVal</warning>;

                var tmp:MyEnum = MyEnum.SECOND;
                var ok3:EnumValue = tmp;
                var ok4:EnumValue = getEnum();

 		var <error descr="Incompatible type: MyEnum should be EnumValue">wrong1:EnumValue = MyEnum</error>;
 		var <error descr="Incompatible type: Bool should be EnumValue">wrong2:EnumValue = Enum<MyEnum></error><error descr="<expression> expected, got ';'">;</error>
 	}

 	public function getEnum():MyEnum {
 	  return MyEnum.THIRD;
 	}
}
enum MyEnum {
    FIRST;
    SECOND;
    THIRD;
}
