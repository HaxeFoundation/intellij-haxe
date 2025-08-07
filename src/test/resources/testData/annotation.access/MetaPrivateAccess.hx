class MetaPrivateAccess {
    public function test() {
        var publicInstance:PublicStaticMembers = null;
        var privateInstance:PrivateStaticMembers = null;
        var defaultInstance:DefaultStaticMembers = null;

        publicInstance.publicVar; // OK
        @:privateAccess privateInstance.privateVar; // OK got access meta
        @:privateAccess defaultInstance.defaultVar;  // OK got access meta

        privateInstance.<error descr="Cannot access private field privateVar">privateVar</error>; //WRONG : no access
        defaultInstance.<error descr="Cannot access private field defaultVar">defaultVar</error>;  //WRONG : no access

        publicInstance.publicMethod(); // OK
            @:privateAccess privateInstance.privateMethod(); // OK got access meta
            @:privateAccess defaultInstance.defaultMethod();  // OK got access meta

        privateInstance.<error descr="Cannot access private field privateMethod">privateMethod</error>(); //WRONG : no access
        defaultInstance.<error descr="Cannot access private field defaultMethod">defaultMethod</error>();  //WRONG : no access


//        // Mixed  public private chain
            //OK
            @:privateAccess publicInstance.publicVar.privateVar.publicProp.defaultMethod();
            @:privateAccess publicInstance.publicProp.defaultVar.publicVar.privateMethod();
            @:privateAccess publicInstance.publicMethod().privateVar.publicVar;

            //Wrong
        publicInstance.publicVar.<error descr="Cannot access private field privateVar">privateVar</error>.publicProp.<error descr="Cannot access private field defaultMethod">defaultMethod</error>();
        publicInstance.publicProp.<error descr="Cannot access private field defaultVar">defaultVar</error>.publicVar.<error descr="Cannot access private field privateMethod">privateMethod</error>();
        publicInstance.publicMethod().<error descr="Cannot access private field privateVar">privateVar</error>.publicVar;

    }
}

class PublicStaticMembers  {
    public var publicVar:PrivateStaticMembers;
    public var publicProp(default,default):DefaultStaticMembers;
    public function publicMethod():PrivateStaticMembers {
        @:privateAccess var x2= publicVar.privateVar; // OK
        var x1= publicVar.<error descr="Cannot access private field privateVar">privateVar</error>; // WRONG
        return <weak_warning descr="Return type can be changed to 'Null<PrivateStaticMembers>' to show nullability">null</weak_warning>;
    }
}

class PrivateStaticMembers extends DefaultStaticMembers  {
    private var privateVar:PublicStaticMembers;
    private var privateProp(default,default):DefaultStaticMembers;
    private function privateMethod():Void{
        // OK (inheretance)
        var x= this.privateProp.defaultVar;
    }
}

class DefaultStaticMembers  {
    var defaultVar:PublicStaticMembers;
    var defaultProp(default,default):PrivateStaticMembers;
    function defaultMethod():Void{
        // OK
        @:privateAccess var x= this.defaultProp.privateVar;
        // WRONG ( note : inheretance only works one way)
        var x= this.defaultProp.<error descr="Cannot access private field privateVar">privateVar</error>;
    }
}
