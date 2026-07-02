package com.intellij.plugins.haxe.lang.completion;


import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class PropertyAcessorCompletionTest extends HaxeCompletionTestBase {
  public PropertyAcessorCompletionTest() {
    super("completion", "property");
  }

  @Test
  public void testGet1() throws Throwable {
    doTest();
  }
  @Test
  public void testGet2() throws Throwable {
    doTest();
  }
  @Test
  public void testGet3() throws Throwable {
    doTest();
  }
  @Test
  public void testSet1() throws Throwable {
    doTest();
  }
  public void testSet2() throws Throwable {
    doTest();
  }


}
