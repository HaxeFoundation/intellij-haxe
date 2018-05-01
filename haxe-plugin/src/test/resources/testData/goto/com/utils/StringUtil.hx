package com.utils;

class StringUtil {
  public static function isNotEmpty(str:String):Bool {
    return str != null && str.length > 0;
  }
}
