package accesscontrol;

@:allow(String)
@:allow(accesscontrol.TestClassAllowOnClass)
@:allow(Array)
class PrivateStaticMembers {

    // package level access
    @:allow(accesscontrol)
    private var privateVarA:Int;

    // class level access
    @:allow(accesscontrol.TestClassAllowOnField)
    private var privateVarB:Int;

    // module member access
    @:allow(accesscontrol.moduleLevelTest)
    private var privateVarC:Int;

    // method/member Level
    @:allow(TestMethodAllowOnProperty.test)
    private var privateProp(default,default):Int;

    // module access (haxe 4.3.6 does not allow module name in path it seems, so this one fails)
    @:allow(accesscontrol.MetaAccess)
    private function privateMethodA():Void{}

    private function privateMethodB():Void{}

}
