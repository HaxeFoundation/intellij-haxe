package;
using <info descr="null">PrivateExtensions</info>;
using <info descr="null"><info descr="null">PrivateExtensions</info>.Base</info>;
using <info descr="null"><info descr="null">PrivateExtensions</info>.Tools</info>;

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

class <info descr="null">PointTools</info> {
    static function <info descr="null">doubled</info>(<info descr="null">p</info>:<info descr="null">Point</info>) return <info descr="null">p</info>.<info descr="null">x</info> * 2;
    public static function <info descr="null">viaTools</info>() return <info descr="null">new</info> <info descr="null">Point</info>().<info descr="null">doubled</info>();  // a private @:using extension, inside its class
}
