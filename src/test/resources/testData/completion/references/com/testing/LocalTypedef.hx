package com.testing;

typedef BarData = com.util.Bar;

class LocalTypedef {
  private function print(bar:BarData){
    bar.<caret>
  }
}
