package com.intellij.plugins.haxe.lang.completion;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Completion: property acessor")
public class PropertyAcessorCompletionTest extends HaxeCompletionTestBase {
  public PropertyAcessorCompletionTest() {
    super("completion", "property");
  }

  @Test
  @DisplayName("get 1")
  public void testGet1() throws Throwable {
    doTest();
  }
  @Test
  @DisplayName("get 2")
  public void testGet2() throws Throwable {
    doTest();
  }
  @Test
  @DisplayName("get 3")
  public void testGet3() throws Throwable {
    doTest();
  }
  @Test
  @DisplayName("set 1")
  public void testSet1() throws Throwable {
    doTest();
  }
  @Test
  @DisplayName("set 2")
  public void testSet2() throws Throwable {
    doTest();
  }


}
