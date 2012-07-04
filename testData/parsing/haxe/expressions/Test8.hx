class Test8 {
  public function example():Something {
      return if (isFlag)
          flagExample;
      else
          noFlagExample;
  }

  public function example2():Something {
    return try example() catch(e:String) {
      null;
    }
  }
}