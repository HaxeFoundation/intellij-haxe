package accesscontrol;
// IMPORTANT!:
// it seems like refrences in allow meta for some reason can not contain Module name as part of the
// fully qualified path or at least thats how it works in haxe 4.3.6, this does not work will with
// logic for resolving refrences and for creating fully qualified path.

class TestClassAllowOnField {
    public function test() {
        var privateInstance:PrivateStaticMembers = null;
        privateInstance.privateVarA ; // OK (package level access)
        privateInstance.privateVarB;   // OK (entire class has access to this field)

//        haxe 4.3.6 does not allow module name in path it seems, so this one fails
//        privateInstance.privateMethodA();   // OK (module level access)

        privateInstance.<error descr="Cannot access private field privateVarC">privateVarC</error>;   // WRONG (no access for this member)
        privateInstance.<error descr="Cannot access private field privateProp">privateProp</error>; // WRONG (no access for this member)
        privateInstance.<error descr="Cannot access private field privateMethodB">privateMethodB</error>(); // WRONG (no access for this member)
    }
}