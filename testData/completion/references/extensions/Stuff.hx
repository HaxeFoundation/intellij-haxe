package com.extensions;

interface IFoo {}
class Foo implements IFoo {}
class Bar extends Foo {}
class Jar extends Bar {}
class Alone {}

class Util {
  public static function ifooOperation(ifoo:IFoo):Void {}
  public static function fooOperation(foo:Foo):Void {}
  public static function barOperation(bar:Bar):Void {}
  public static function jarOperation(jar:Jar):Void {}
  public static function aloneOperation(alone:Alone):Void {}
}