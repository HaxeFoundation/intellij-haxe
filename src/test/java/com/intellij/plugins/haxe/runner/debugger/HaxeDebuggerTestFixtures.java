package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.testFramework.fixtures.CodeInsightTestFixture;

/**
 * The fixture projects the debugger tests stop their imaginary frames in.
 * Each builds a small project and leaves the caret on the breakpoint line.
 */
final class HaxeDebuggerTestFixtures {

  private HaxeDebuggerTestFixtures() {
  }

  /**
   * Instance frame: stopped inside Widget.update(), where {@code this} is a
   * Widget extends Base (one inherited var + one inherited method).
   */
  static void instanceFrameProject(CodeInsightTestFixture fixture) {
    fixture.addFileToProject("Base.hx",
                             "class Base { public var inherited:Int = 2; public function baseAction():Void {} }");
    fixture.configureByText("Widget.hx", """
      class Widget extends Base { var count:Int = 1;
        function update() { trace<caret>(count); }
        static function main() { new Widget().update(); } }""");
  }

  /**
   * Same instance frame, but stopped INSIDE a callback defined in an object
   * literal. Object literals are HaxeClass in the PSI but don't rebind
   * {@code this} in Haxe, so fragment-context fallbacks must walk past them —
   * the literal is the first HaxeClass the context walk finds.
   */
  static void objectLiteralFrameProject(CodeInsightTestFixture fixture) {
    fixture.addFileToProject("Base.hx", "class Base { public var inherited:Int = 2; }");
    fixture.configureByText("Widget.hx", """
      class Widget extends Base { var count:Int = 1;
        function update() { var o = { cb: function() { trace<caret>(0); } }; }
        static function main() { new Widget().update(); } }""");
  }

  /** Shape/Circle in their own files (both PRIMARY classes), stopped in Main with `var s:Shape = new Circle()`. */
  static void shapesProject(CodeInsightTestFixture fixture) {
    fixture.addFileToProject("shapes/Shape.hx",
                             "package shapes;\nclass Shape { public var base:Int = 0; }");
    fixture.addFileToProject("shapes/Circle.hx",
                             "package shapes;\nclass Circle extends Shape { public var radius:Float = 1; }");
    fixture.configureByText("Main.hx", """
      import shapes.Shape;
      import shapes.Circle;
      class Main { static function main() { var s:Shape = new Circle(); trace<caret>(s); } }""");
  }

  /** A module whose ancillary class Secondary is what the debugger reports as `pack.Secondary`. */
  static void ancillaryProject(CodeInsightTestFixture fixture) {
    fixture.addFileToProject("pack/Module.hx",
                             "package pack;\nclass Module {}\nclass Secondary { public var marker:Int = 1; }");
    fixture.configureByText("Main.hx",
                            "class Main { static function main() { var o = null; trace<caret>(o); } }");
  }
}
