package completion.references;

import extensions.Gated.Gate;

using extensions.Gated.Tools;

class Extensions7 {
  @:access(extensions.Gated.Tools)
  static function main():Void {
    new Gate().<caret>
  }
}
