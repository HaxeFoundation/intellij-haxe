package com.intellij.plugins.haxe.model.type;

public enum UnificationRules {
  DEFAULT,
  PREFER_VOID,// typically when finding method return types
  IGNORE_VOID, // typically when finding type for array and map literal (with guards)
  PREFER_DYNAMIC,
  //  `var a = null; a = 1;`  should unify to null<Int>,
  //  but `var a = 1 + null` should (most likely) never unify (illegal operation)
  UNIFY_NULL,
}