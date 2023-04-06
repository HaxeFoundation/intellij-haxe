package ;
using <info descr="">Std</info>;

class <info descr="">TestIssue964_StaticExtensionsUnresolved</info> {
    var <info descr="">m</info>:<info descr="">Map</info><<info descr="">String</info>,<info descr="">String</info>> = <info descr="">new</info> <info descr="">Map</info>();
    public function <info descr="">new</info>() {
        <info descr=""><info descr=""><info descr="">m</info>.get</info>("someString").parseInt</info>();   // <<---- parseInt() marked as unresolved.
        <info descr=""><info descr="">m</info>.is</info>(<info descr="">Map</info>);
    }
}
