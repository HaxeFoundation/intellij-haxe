class Test {
  public static function main()
  {
    var a = [0,1,2,3];
    for (i in a)
      trace(i);

    a = [
      for(i in 0...10)
        i
      ];
    for (i in a)
      trace(i);

    for (i in 0...10)
      trace(i);
  }
}