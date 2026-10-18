package com.intellij.plugins.haxe.resolve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Member access resolves through the receiver's element type: map and array access, Null and abstract wrappers, the for-loop variable. */
@DisplayName("Resolve: annotation")
public class HaxeResolveAnnotationTest extends HaxeResolveHighlightingTestBase {

  @Override
  protected String getBasePath() {
    return "/resolve/annotation/";
  }

  @Test
  @DisplayName("value type unresolved on dynamic map")
  public void testValueTypeUnresolvedOnDynamicMap() {
    doTest();
  }

  @Test
  @DisplayName("value type known from type tag on map access")
  public void testValueTypeKnownFromTypeTagOnMapAccess() {
    doTest();
  }

  @Test
  @DisplayName("value type known on map access")
  public void testValueTypeKnownOnMapAccess() {
    doTest();
  }

  @Test
  @DisplayName("value type inferred on map access")
  public void testValueTypeInferredOnMapAccess() {
    doTest();
  }

  @Test
  @DisplayName("inferred type on array access")
  public void testInferredTypeOnArrayAccess() {
    doTest();
  }

  @Test
  @DisplayName("known type on array access")
  public void testKnownTypeOnArrayAccess() {
    doTest();
  }

  @Test
  @DisplayName("known type from type tag on array access")
  public void testKnownTypeFromTypeTagOnArrayAccess() {
    doTest();
  }

  @Test
  @DisplayName("dynamic array cant be accessed")
  public void testDynamicArrayCantBeAccessed() {
    doTest();
  }

  @Test
  @DisplayName("method access through nullable")
  public void testMethodAccessThroughNullable() {
    doTest();
  }

  @Test
  @DisplayName("var access through nullable")
  public void testVarAccessThroughNullable() {
    doTest();
  }

  @Test
  @DisplayName("method access through abstract")
  public void testMethodAccessThroughAbstract() {
    doTest();
  }

  @Test
  @DisplayName("method access through abstract and null")
  public void testMethodAccessThroughAbstractAndNull() {
    doTest();
  }

  /** ArrayAccess and ArrayIterator come from the mounted std. */
  @Test
  @DisplayName("for loop variable type")
  public void testForLoopVariableType() {
    doTest();
  }
}
