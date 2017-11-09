package completion.references;

import extensions.Stuff.IFoo;

using extensions.Stuff.Util;

class Extensions1 {
  static function main():Void {
    var ifoo:IFoo;
    ifoo.<caret>
  }
}