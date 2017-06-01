class Test {
  static function main() {
    var i1:Int = 8;
    var i2:Int = 4;
    var i3:Int = 2;

    i1 = i3 = i2 >>>= 1;
    i1 >>= i2 >>= 1;
    i1 += i3 <<= 1;
    i1 *= i3 % i2 << 1;
  }
}