package;
class Test {
  final i:Int = 0;
  public function main() {
    <error descr="Cannot assign value to 'final' variable.">i = 1</error>;
  }
}
