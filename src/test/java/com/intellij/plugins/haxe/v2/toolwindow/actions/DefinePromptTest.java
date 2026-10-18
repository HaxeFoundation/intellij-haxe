package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Tool window: define prompt")
public class DefinePromptTest extends HaxeLightFixtureTestCase {

  private static final String NOT_A_NAME = HaxeBundle.message("haxe.toolwindow.define.name.invalid");
  private static final String NO_VALUE_PART = HaxeBundle.message("haxe.toolwindow.add.undefine.invalid");

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (entered text, Add Define error, Add Undefine error; null = accepted). */
  static final List<Arguments> ENTRIES = List.of(
    arguments("my_flag", null, null),
    arguments(" my_flag ", null, null),
    // a value part belongs to Add Define only
    arguments("my_flag=1", null, NO_VALUE_PART),
    arguments("", NOT_A_NAME, NOT_A_NAME),
    arguments("my flag", NOT_A_NAME, NOT_A_NAME),
    // the compiler identity defines cannot be overridden, in either spelling
    arguments("haxe_ver", identity("haxe_ver"), identity("haxe_ver")),
    arguments("haxe-ver=4.3", identity("haxe-ver"), NO_VALUE_PART),
    arguments("haxe4", identity("haxe4"), identity("haxe4")),
    arguments("haxe", identity("haxe"), identity("haxe")));

  @ParameterizedTest(name = "\"{0}\"")
  @FieldSource("ENTRIES")
  @DisplayName("validates define and undefine entries")
  public void testValidatesDefineAndUndefineEntries(String entered, String defineError, String undefineError) {
    assertEquals(defineError, DefinePrompt.defineInputError(entered));
    assertEquals(undefineError, DefinePrompt.undefineNameError(entered));
  }

  private static String identity(String name) {
    return HaxeBundle.message("haxe.toolwindow.define.name.identity", name);
  }
}
