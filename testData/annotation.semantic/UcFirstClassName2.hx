class test extends <error descr="Type name 'test1' must start with uppercase">test1</error> {
  public function demo(a:<error descr="Type name 'test' must start with uppercase">test</error>, b:<error descr="Type name 'test2' must start with uppercase">test2</error>, c:<error descr="Type name 'string' must start with uppercase">string</error>) {

  }
}