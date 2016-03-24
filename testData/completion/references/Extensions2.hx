package completion.references;

import com.extensions.Stuff.Foo;

using com.extensions.Stuff.Util;

class Extensions2 {
  static function main():Void {
    new Foo().<caret>
  }
}