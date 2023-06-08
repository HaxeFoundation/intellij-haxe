package ;
using <text_attr descr="null">Std</text_attr>;

class <text_attr descr="null">TestIssue964_StaticExtensionsUnresolved</text_attr> {
    var <text_attr descr="null">m</text_attr>:<text_attr descr="null">Map</text_attr><<text_attr descr="null">String</text_attr>,<text_attr descr="null">String</text_attr>> = <text_attr descr="null">new</text_attr> <text_attr descr="null">Map</text_attr>();
    public function <text_attr descr="null">new</text_attr>() {
        <text_attr descr="null">m</text_attr>.<text_attr descr="null">get</text_attr>("someString").<text_attr descr="null">parseInt</text_attr>();   // <<---- parseInt() marked as unresolved.
        <text_attr descr="null">m</text_attr>.<text_attr descr="null">is</text_attr>(<text_attr descr="null">Map</text_attr>);
    }
}
