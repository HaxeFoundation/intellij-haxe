class Main {
  static public function main1(test:StringFromTo = 'test') {
  }

  static public function main2(test:StringFrom = 'test') {
  }

  static public function main3(<error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>StringTo</b></td></tr><tr><td>Found:</td><td><font color='red'><b>String</b></font></td></tr></table></body></html>">test:StringTo = 'test'</error>) {
  }
}

abstract StringFromTo from String to String {
}

abstract StringFrom from String {
}

abstract StringTo to String {
}
