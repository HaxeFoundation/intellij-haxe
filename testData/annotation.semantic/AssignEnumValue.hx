package;

class <info descr="null">AssignEnumValue</info> {
 	public function new() {

 		var ok1:EnumValue = MyEnum.FIRST;
 		var ok2:EnumValue = enuVal;

                var tmp:MyEnum = MyEnum.SECOND;
                var ok3:EnumValue = tmp;
                var ok4:EnumValue = getEnum();

 		var <error descr="Incompatible type: MyEnum should be EnumValue">wrong1:EnumValue = MyEnum</error>;
 		var <error descr="Incompatible type: Bool should be EnumValue">wrong2:EnumValue = Enum<MyEnum<error descr="'>' unexpected">></error></error>;
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
