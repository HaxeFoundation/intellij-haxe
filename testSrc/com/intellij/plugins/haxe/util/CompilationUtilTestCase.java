package com.intellij.plugins.haxe.util;

import org.junit.Assert;
import org.junit.Test;

public class CompilationUtilTestCase extends Assert {

  @Test
  public void doTestWindows() {
    doTest("Main", "C:\\Program Files (x86)\\JetBrains\\Main.hx");
  }

  @Test
  public void doTestUnix() {
    doTest("Main", "/home/fedor/JetBrains/Main.hx");
  }

  private void doTest(String expectedClassName, String classPath) {
    String className = CompilationUtil.getClassNameByPath(classPath);
    assertEquals(className, expectedClassName);
  }
}
