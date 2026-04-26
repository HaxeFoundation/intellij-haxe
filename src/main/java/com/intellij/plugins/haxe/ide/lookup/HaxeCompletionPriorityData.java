package com.intellij.plugins.haxe.ide.lookup;

public class HaxeCompletionPriorityData {
  public static final double FUNCTION_TYPE = 4;
  public static final double LOCAL_VAR = 3;
  public static final double FIELD = 2;
  public static final double METHOD = 1;

  public double name;
  public double type;
  public double assignable;
  public double accessor;

  public boolean done = false;

  public double calculate() {
    return name + type + assignable + accessor + accessor + assignable;
  }

}
