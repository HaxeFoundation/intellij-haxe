package;
using <info descr="null">PrivateExtensions</info>;
using <info descr="null"><info descr="null">PrivateExtensions</info>.Base</info>;
using <info descr="null"><info descr="null">PrivateExtensions</info>.Tools</info>;
using <info descr="null"><info descr="null">PrivateExtensions</info>.Gated</info>;

class <info descr="null">PrivateExtensions</info> {
    static function <info descr="null">main</info>() {
        var <info descr="null">doubled</info> = 1.<info descr="null">twice</info>();  // its own private extension
        var <info descr="null">tripled</info> = 1.<warning descr="Unresolved symbol">thrice</warning><info descr="">()</info>;  // another class's private extension
        var <info descr="null">outside</info> = <info descr="null">new</info> <info descr="null">Point</info>().<warning descr="Unresolved symbol">doubled</warning><info descr="">()</info>;  // a private @:using extension, outside its class
    }
    static function <info descr="null">twice</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> * 2;
}

class <info descr="null">Base</info> {
    static function <info descr="null">half</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> >> 1;
}

class <info descr="null">Derived</info> extends <info descr="null">Base</info> {
    static function <info descr="null">run</info>() {
        var <info descr="null">halved</info> = 4.<info descr="null">half</info>();  // a private extension of the superclass
    }
}

class <info descr="null">Tools</info> {
    static function <info descr="null">thrice</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> * 3;
}

@:using(<info descr="null"><info descr="null">PrivateExtensions</info>.PointTools</info>)
class <info descr="null">Point</info> {
    public var <info descr="null">x</info>:<info descr="null">Int</info> = 1;
    public function <info descr="null">new</info>() {}
}

class <info descr="null">Gated</info> {
    @:allow(<info descr="null"><info descr="null">PrivateExtensions</info>.AllowedCaller</info>)
    static function <info descr="null">quarter</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> >> 2;
    static function <info descr="null">fifth</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> * 5;
    static function <info descr="null">sixth</info>(<info descr="null">i</info>:<info descr="null">Int</info>) return <info descr="null">i</info> * 6;
}

class <info descr="null">AllowedCaller</info> {
    static function <info descr="null">run</info>() {
        var <info descr="null">four</info> = 4;
        var <info descr="null">quartered</info> = <info descr="null">four</info>.<info descr="null">quarter</info>();  // @:allow on the extension names this class
    }
}

class <info descr="null">AccessingCaller</info> {
    @:access(<info descr="null"><info descr="null">PrivateExtensions</info>.Gated</info>)
    static function <info descr="null">run</info>() {
        var <info descr="null">five</info> = 5;
        var <info descr="null">fifthed</info> = <info descr="null">five</info>.<info descr="null">fifth</info>();  // @:access on this method names the extension's class
    }
}

class <info descr="null">PrivateAccessCaller</info> {
    static function <info descr="null">run</info>() {
        var <info descr="null">six</info> = 6;
        var <info descr="null">sixthed</info> = @:privateAccess <info descr="null">six</info>.<info descr="null">sixth</info>();  // @:privateAccess on the call
        var <info descr="null">quartered</info> = <info descr="null">six</info>.<warning descr="Unresolved symbol">quarter</warning><info descr="">()</info>;  // no metadata grants access here
    }
}

class <info descr="null">PointTools</info> {
    static function <info descr="null">doubled</info>(<info descr="null">p</info>:<info descr="null">Point</info>) return <info descr="null">p</info>.<info descr="null">x</info> * 2;
    public static function <info descr="null">viaTools</info>() return <info descr="null">new</info> <info descr="null">Point</info>().<info descr="null">doubled</info>();  // a private @:using extension, inside its class
}
