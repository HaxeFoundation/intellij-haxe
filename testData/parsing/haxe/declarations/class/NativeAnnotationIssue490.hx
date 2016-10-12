@:native("Test")
extern class NativeAnnotationIssue490 {
  @:native("foo") function bar(): Void;
  @:native("a") var b: Int;
}
