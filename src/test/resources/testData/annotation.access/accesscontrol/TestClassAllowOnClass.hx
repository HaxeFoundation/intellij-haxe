package accesscontrol;
// IMPORTANT!:
// it seems like refrences in allow meta for some reason can not contain Module name as part of the
// fully qualified path or at least thats how it works in haxe 4.3.6, this does not work will with
// logic for resolving refrences and for creating fully qualified path.

class TestClassAllowOnClass {
    public function test() {
        var privateInstance:PrivateStaticMembers  = null;
        privateInstance.privateVarA; // OK (entire class has access to all members)
        privateInstance.privateVarB ; // OK (entire class has access to all members)
        privateInstance.privateVarC ; // OK (entire class has access to all members)
        privateInstance.privateProp; // OK (entire class has access to all members)
        privateInstance.privateMethodA();   // OK (entire class has access to all members)
        privateInstance.privateMethodB();   // OK (entire class has access to all members)
    }
}