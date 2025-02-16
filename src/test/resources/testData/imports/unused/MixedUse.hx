// used
import <info descr="null">helper.Bar</info>;
import <info descr="null">helper.Foo</info>;
import <info descr="null">helper.IFoo</info> <info descr="null">as</info> FooAlias;

// unused
<warning descr="Unused import statement">import <info descr="null"><info descr="null">helper.Typedefs</info>.AliasTypedef</info>;</warning>

class <info descr="null">OptimizeImportsTest</info> extends <info descr="null">Bar</info> {

public var <info descr="null">member1</info>:<info descr="null">helper.Bar</info>;
public var <info descr="null">member2</info>:<info descr="null">Foo</info>;
public var <info descr="null">member3</info>:<info descr="null">FooAlias</info>;

}

