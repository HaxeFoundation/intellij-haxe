package ;
using <info descr="null">Std</info>;

class <info descr="null">TestIssue964_StaticExtensionsUnresolved</info> {
    var <info descr="null">m</info>:<info descr="null">Map</info><<info descr="null">String</info>,<info descr="null">String</info>> = <info descr="null">new</info> <info descr="null">Map</info>();
    public function <info descr="null">new</info>() {
        <info descr="null">m</info>.<info descr="null">get</info>("someString").<info descr="null">parseInt</info>();   // <<---- parseInt() marked as unresolved.
        <info descr="null">m</info>.<info descr="null">is</info>(<info descr="null">Map</info>);
    }
}
