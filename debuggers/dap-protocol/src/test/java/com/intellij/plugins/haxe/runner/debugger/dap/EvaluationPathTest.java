package com.intellij.plugins.haxe.runner.debugger.dap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EvaluationPathTest {
  @Test
  public void rootIsTheNameWhenAnIdentifier() {
    assertEquals("this", EvaluationPath.root("this"));
    assertEquals("count", EvaluationPath.root("count"));
    assertEquals("_x2", EvaluationPath.root("_x2"));
  }

  @Test
  public void rootIsNullWhenNotAnIdentifier() {
    assertNull(EvaluationPath.root("0"));       // an index is never a root local
    assertNull(EvaluationPath.root("…"));       // truncation marker
    assertNull(EvaluationPath.root(""));
    assertNull(EvaluationPath.root(null));
  }

  @Test
  public void namedFieldsJoinWithADot() {
    assertEquals("this.field", EvaluationPath.child("this", "field"));
    assertEquals("a.b.c", EvaluationPath.child(EvaluationPath.child("a", "b"), "c"));
  }

  @Test
  public void arrayIndicesUseBracketsInBothAdapterConventions() {
    assertEquals("arr[0]", EvaluationPath.child("arr", "0"));    // HashLink: bare digits
    assertEquals("arr[12]", EvaluationPath.child("arr", "[12]")); // hxcpp: already bracketed
  }

  @Test
  public void theUsersFullExample() {
    // this > someObject > someArray > [0] > myVar
    String path = "this";
    path = EvaluationPath.child(path, "someObject");
    path = EvaluationPath.child(path, "someArray");
    path = EvaluationPath.child(path, "0");   // HashLink names the element "0"
    path = EvaluationPath.child(path, "myVar");
    assertEquals("this.someObject.someArray[0].myVar", path);
  }

  @Test
  public void inexpressibleSegmentsPoisonTheWholeSubtree() {
    // a map keyed by a non-identifier string has no valid access path
    String mapEntry = EvaluationPath.child("myMap", "some key");
    assertNull(mapEntry);
    // and every descendant of it is null too, so no bogus prefill
    assertNull(EvaluationPath.child(mapEntry, "field"));
  }

  @Test
  public void childOfNullIsNull() {
    assertNull(EvaluationPath.child(null, "field"));
    assertNull(EvaluationPath.child(null, "0"));
  }
}
