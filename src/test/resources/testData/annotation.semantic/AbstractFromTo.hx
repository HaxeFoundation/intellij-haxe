class Main {
  static public function main1(test:StringFromTo = 'test') {
  }

  static public function main2(test:StringFrom = 'test') {
  }

  static public function main3(<error descr="Incompatible type: String should be StringTo">test:StringTo = 'test'</error>) {
  }
}

abstract StringFromTo from String to String {
}

abstract StringFrom from String {
}

abstract StringTo to String {
}
