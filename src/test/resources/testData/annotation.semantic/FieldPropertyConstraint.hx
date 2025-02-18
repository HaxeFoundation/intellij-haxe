package ;

typedef TestTypedef = {var value(get, set ):Int;}

class FieldPropertyConstraintsTest {
    public function new() {
        var normalField:NormalField = null;
        var defaultProperty:DefaultProperty = null;
        var nullProperty:NullProperty = null;
        var isVarProperty:IsVarProperty = null;
        var getterSetterProperty:GetterSetterProperty = null;

        //CORRECT
        fieldConstraint(normalField);
        fieldConstraint(defaultProperty );
        // WRONG
        fieldConstraint(<error descr="Type mismatch (Expected: '{value:Int}' got: 'NullProperty')">nullProperty</error>); // Inconsistent access for field value : (null,null) should be (default,default)
        fieldConstraint(<error descr="Type mismatch (Expected: '{value:Int}' got: 'IsVarProperty')">isVarProperty</error>); // Inconsistent access for field value : (get,set) should be (default,default)
        fieldConstraint(<error descr="Type mismatch (Expected: '{value:Int}' got: 'GetterSetterProperty')">getterSetterProperty</error>); // Inconsistent access for field value : (get,set) should be (default,default)
//
//        //CORRECT
        defaultPropertyConstraint(normalField);
        defaultPropertyConstraint(defaultProperty);
//        //WRONG
        defaultPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(default, default):Int;}' got: 'NullProperty')">nullProperty</error>);// Inconsistent access for field value : (null,null) should be (default,default)
        defaultPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(default, default):Int;}' got: 'IsVarProperty')">isVarProperty</error>);// Inconsistent access for field value : (get,set) should be (default,default)
        defaultPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(default, default):Int;}' got: 'GetterSetterProperty')">getterSetterProperty</error>);// Inconsistent access for field value : (get,set) should be (default,default)
//
//        //CORRECT
        getterSetterPropertyConstraint(isVarProperty);
        getterSetterPropertyConstraint(getterSetterProperty);
//        //WRONG
        getterSetterPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(get, set ):Int;}' got: 'NormalField')">normalField</error>);// Inconsistent access for field value : (default,default) should be (get,set)
        getterSetterPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(get, set ):Int;}' got: 'DefaultProperty')">defaultProperty</error>); //Inconsistent access for field value : (default,default) should be (get,set)
        getterSetterPropertyConstraint(<error descr="Type mismatch (Expected: '{var value(get, set ):Int;}' got: 'NullProperty')">nullProperty</error>); //Inconsistent access for field value : (null,null) should be (get,set)


    }

    function fieldConstraint<T:{value:Int}>(arg:T) {}
    function defaultPropertyConstraint<T: {var value(default, default):Int;}>(arg:T) {}
    function getterSetterPropertyConstraint<T: {var value(get, set ):Int;}>(arg:T) {}

}


class NormalField {
    public var value:Int;
}
class DefaultProperty {
    public var value(default, default):Int;
}
class NullProperty {
    public var value(null, null):Int;
}
class GetterSetterProperty {
    public var value(get, set):Int;

    function set_value(value:Int):Int {return value;}
    function get_value():Int {return 1;}
}
class IsVarProperty {
    @:isVar public var value(get, set):Int;

    function set_value(value:Int):Int {return this.value = value;}
    function get_value():Int {return value;}
}

