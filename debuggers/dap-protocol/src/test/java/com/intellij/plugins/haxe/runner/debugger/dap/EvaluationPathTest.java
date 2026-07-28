package com.intellij.plugins.haxe.runner.debugger.dap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("DAP protocol: evaluation path")
public class EvaluationPathTest {
  @Test
  @DisplayName("root is the name when an identifier")
  public void rootIsTheNameWhenAnIdentifier() {
    assertEquals("this", EvaluationPath.root("this"));
    assertEquals("count", EvaluationPath.root("count"));
    assertEquals("_x2", EvaluationPath.root("_x2"));
  }

  @Test
  @DisplayName("root is null when not an identifier")
  public void rootIsNullWhenNotAnIdentifier() {
    assertNull(EvaluationPath.root("0"));       // an index is never a root local
    assertNull(EvaluationPath.root("…"));       // truncation marker
    assertNull(EvaluationPath.root(""));
    assertNull(EvaluationPath.root(null));
  }

  @Test
  @DisplayName("named fields join with a dot")
  public void namedFieldsJoinWithADot() {
    assertEquals("this.field", EvaluationPath.child("this", "field"));
    assertEquals("a.b.c", EvaluationPath.child(EvaluationPath.child("a", "b"), "c"));
  }

  @Test
  @DisplayName("array indices use brackets in both adapter conventions")
  public void arrayIndicesUseBracketsInBothAdapterConventions() {
    assertEquals("arr[0]", EvaluationPath.child("arr", "0"));    // HashLink: bare digits
    assertEquals("arr[12]", EvaluationPath.child("arr", "[12]")); // hxcpp: already bracketed
  }

  @Test
  @DisplayName("the users full example")
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
  @DisplayName("inexpressible segments poison the whole subtree")
  public void inexpressibleSegmentsPoisonTheWholeSubtree() {
    // a map keyed by a non-identifier string has no valid access path
    String mapEntry = EvaluationPath.child("myMap", "some key");
    assertNull(mapEntry);
    // and every descendant of it is null too, so no bogus prefill
    assertNull(EvaluationPath.child(mapEntry, "field"));
  }

  @Test
  @DisplayName("child of null is null")
  public void childOfNullIsNull() {
    assertNull(EvaluationPath.child(null, "field"));
    assertNull(EvaluationPath.child(null, "0"));
  }
}
