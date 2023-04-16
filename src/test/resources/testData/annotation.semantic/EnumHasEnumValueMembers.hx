package ;
enum MyEnum { Foo; Bar; }
class Demo {
    public static function main()
    {
        var foo = MyEnum.Foo;
        var bar:MyEnum = Bar;

        // `match` is not resolved:
        MyEnum.Foo.match(MyEnum.Foo);
        Foo.match(MyEnum.Foo);
        foo.match(MyEnum.Bar);
        bar.match(Foo);
    }
}