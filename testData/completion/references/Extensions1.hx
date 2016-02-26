package completion.references;

import com.extensions.Stuff.IFoo;

using com.extensions.Stuff.Util;

class Extensions1 {
  static function main():Void {
    var ifoo:IFoo;
    ifoo.<caret>
  }
}