package
class RegularExpression {
  function foo(){
    var tmp = ~/Hello/;
    tmp.mat<caret>ch("Hello");
  }
}