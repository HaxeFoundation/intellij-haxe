class AccessModifiersChainAccess {
    public function test() {
        var instance:PublicStaticMembers = null;
        //only privateXX and defaultXX parts of chain should give warning
        instance.publicVar.<error descr="Cannot access private field privateVar">privateVar</error>.publicProp.<error descr="Cannot access private field defaultMethod">defaultMethod</error>();
        instance.publicProp.<error descr="Cannot access private field defaultVar">defaultVar</error>.publicVar.<error descr="Cannot access private field privateMethod">privateMethod</error>();
        instance.publicMethod().<error descr="Cannot access private field privateVar">privateVar</error>.publicVar;
    }
}

class PublicStaticMembers  {
    public var publicVar:PrivateStaticMembers;
    public var publicProp(default,default):DefaultStaticMembers;
    public function publicMethod():PrivateStaticMembers{
        var x= publicVar.<error descr="Cannot access private field privateVar">privateVar</error>;
        return <weak_warning descr="Return type can be changed to 'Null<PrivateStaticMembers>' to show nullability">null</weak_warning>;
    }
}
class PrivateStaticMembers  {
    private var privateVar:PublicStaticMembers;
    private var privateProp(default,default):DefaultStaticMembers;
    private function privateMethod():Void{var x= this.privateVar.publicVar;}
}

class DefaultStaticMembers  {
    var defaultVar:PublicStaticMembers;
    var defaultProp(default,default):PrivateStaticMembers;
    function defaultMethod():Void{var x= this.defaultVar.publicVar;}
}


