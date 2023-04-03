package;

final <error descr="final must not be repeated for class declaration">final</error> class Test1 {}
extern <error descr="extern must not be repeated for class declaration">extern </error>class Test2{}
private <error descr="private must not be repeated for class declaration">private</error> class Test3{}
private final extern class Test4{}
private final extern <error descr="private must not be repeated for class declaration">private</error> class Test5{}
final private extern <error descr="private must not be repeated for class declaration">private</error> <error descr="final must not be repeated for class declaration">final</error> class Test6{}