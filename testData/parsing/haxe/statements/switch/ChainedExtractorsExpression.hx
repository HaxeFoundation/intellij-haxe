package;

class ChainedExtractorExpression {
       public function test() {
          switch (3) {
            case add(_, 1) => mul(_, 3) => a:
          trace(a);
        }
      }

      function add(i1:Int, i2:Int) {
      return i1 + i2;
      }

      function mul(i1:Int, i2:Int) {
      return i1 * i2;
      }
}