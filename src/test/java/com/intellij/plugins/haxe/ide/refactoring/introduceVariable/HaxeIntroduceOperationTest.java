package com.intellij.plugins.haxe.ide.refactoring.introduceVariable;

import com.intellij.plugins.haxe.util.HaxeNameKind;
import com.intellij.plugins.haxe.util.HaxeSuggestedNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Refactoring: introduce operation")
public class HaxeIntroduceOperationTest {

  @Test
  @DisplayName("a suggested name counts as suggested")
  public void testASuggestedNameCountsAsSuggested() {
    HaxeIntroduceOperation operation = new HaxeIntroduceOperation(null, null, null, null, "introduce");
    operation.setSuggestion(suggestion("name", "spriteName"));

    operation.suggestName();

    assertEquals("name", operation.getName());
    assertTrue(operation.isNameSuggested(), "the in-place template offers every suggestion only for a suggested name");
  }

  @Test
  @DisplayName("a name set afterwards no longer counts as suggested")
  public void testANameSetAfterwardsNoLongerCountsAsSuggested() {
    HaxeIntroduceOperation operation = new HaxeIntroduceOperation(null, null, null, null, "introduce");
    operation.setSuggestion(suggestion("name"));
    operation.suggestName();

    operation.setName("chosen");

    assertFalse(operation.isNameSuggested());
  }

  private static HaxeSuggestedNames suggestion(String... names) {
    return new HaxeSuggestedNames(List.of(names), HaxeNameKind.VARIABLE, null, null);
  }
}
