package completion.references;

import extensions.Stuff.Foo;

using extensions.Stuff.Util;

class Extensions2 {
  static function main():Void {
    new Foo().<caret>
  }
}