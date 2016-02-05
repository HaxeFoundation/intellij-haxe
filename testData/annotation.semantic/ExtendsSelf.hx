package ;

typedef TBase = {
  var x:Int;
}

typedef TExtend1 = {
  > TBase,
  var y:Int;
}

typedef TExtendSelf = {
  ><error descr="Cannot extend self">TExtendSelf</error>,
  var z:Int;
}

typedef TBar = {
  >test.TBar
}

class BarBase {}
class BarExtend1 extends BarBase {}
class BarExtendSelf extends <error descr="Cannot extend self">BarExtendSelf</error> {}
class Bar extends test.Bar {}

interface IBarBase {}
interface IBarMultiple extends IBarBase {}
interface IBarExtendSelf extends <error descr="Cannot extend self">IBarExtendSelf</error> {}
interface IBar extends test.IBar {}