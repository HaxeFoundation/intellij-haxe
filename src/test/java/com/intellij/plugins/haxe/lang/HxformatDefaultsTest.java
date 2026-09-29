package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatDefaults;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks {@link HxformatDefaults} against the tool's own defaults. The
 * fixture is what {@code haxelib run formatter --default-config <file>}
 * writes; the file must exist before the command runs. Regenerate it when
 * upgrading the formatter.
 */
@DisplayName("Formatting: haxe-formatter defaults")
public class HxformatDefaultsTest extends HaxeLightFixtureTestCase {
  private JsonNode config;

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  @BeforeEach
  void readDefaultConfig() throws IOException {
    config = new ObjectMapper().readTree(Path.of(getTestDataPath(), "default-hxformat.json").toFile());
  }

  @Test
  @DisplayName("indentation and margin")
  public void testIndentationAndMargin() {
    assertEquals("tab", config.at("/indentation/character").asString());
    assertEquals(HxformatDefaults.TAB_WIDTH, config.at("/indentation/tabWidth").asInt());
    assertEquals(HxformatDefaults.MAX_LINE_LENGTH, config.at("/wrapping/maxLineLength").asInt());
  }

  @Test
  @DisplayName("operator chain rules")
  public void testOperatorChainRules() {
    assertEquals(HxformatDefaults.BOOL_CHAIN_LINE_LENGTH, condition("opBoolChain", 0, "lineLength >= n"));
    assertEquals(HxformatDefaults.BOOL_CHAIN_ITEM_LENGTH, condition("opBoolChain", 0, "anyItemLength >= n"));
    assertEquals(HaxeOperatorChainRules.KEEP_ITEM_COUNT, condition("opBoolChain", 2, "itemCount <= n"));
    assertEquals(HxformatDefaults.BOOL_CHAIN_TOTAL_LENGTH, condition("opBoolChain", 3, "totalItemLength <= n"));
    assertEquals(HxformatDefaults.BOOL_CHAIN_ITEM_COUNT, condition("opBoolChain", 4, "itemCount >= n"));

    assertEquals(HxformatDefaults.ADD_CHAIN_LINE_LENGTH, condition("opAddSubChain", 0, "lineLength >= n"));
    assertEquals(HxformatDefaults.ADD_CHAIN_ITEM_LENGTH, condition("opAddSubChain", 0, "anyItemLength >= n"));
    assertEquals(HaxeOperatorChainRules.KEEP_ITEM_COUNT, condition("opAddSubChain", 2, "itemCount <= n"));
    assertEquals(HxformatDefaults.ADD_CHAIN_TOTAL_LENGTH, condition("opAddSubChain", 3, "totalItemLength <= n"));
    assertEquals(HxformatDefaults.ADD_CHAIN_ITEM_COUNT, condition("opAddSubChain", 4, "itemCount >= n"));
  }

  @Test
  @DisplayName("multi var rules")
  public void testMultiVarRules() {
    assertEquals(HxformatDefaults.MULTI_VAR_FILL_ITEM_LENGTH, condition("multiVar", 0, "anyItemLength <= n"));
    assertEquals(HxformatDefaults.MULTI_VAR_LINE_LENGTH, condition("multiVar", 1, "lineLength >= n"));
  }

  @Test
  @DisplayName("empty lines")
  public void testEmptyLines() {
    JsonNode emptyLines = config.get("emptyLines");
    assertEquals(HxformatDefaults.MAX_BLANK_LINES, emptyLines.get("maxAnywhereInFile").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_AFTER_PACKAGE, emptyLines.get("afterPackage").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_AFTER_IMPORTS, emptyLines.at("/importAndUsing/beforeType").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_IMPORTS, emptyLines.at("/importAndUsing/betweenImports").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_TYPES, emptyLines.get("betweenTypes").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES, emptyLines.get("betweenSingleLineTypes").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_AFTER_FILE_HEADER, emptyLines.get("afterFileHeaderComment").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_MULTILINE_COMMENTS, emptyLines.get("betweenMultilineComments").asInt());
    assertEquals("remove", emptyLines.get("afterLeftCurly").asString());
    assertEquals("remove", emptyLines.get("beforeRightCurly").asString());
    assertEquals("one", emptyLines.get("beforeDocCommentEmptyLines").asString());
    assertEquals("one", emptyLines.get("afterFieldsWithDocComments").asString());

    JsonNode classLines = emptyLines.get("classEmptyLines");
    assertEquals(HxformatDefaults.BLANK_LINES_BEGIN_TYPE, classLines.get("beginType").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_END_TYPE, classLines.get("endType").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_VARS, classLines.get("betweenVars").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_FUNCTIONS, classLines.get("betweenFunctions").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_VAR_GROUPS, classLines.get("afterStaticVars").asInt());
    assertEquals(HxformatDefaults.BLANK_LINES_BETWEEN_VAR_GROUPS, classLines.get("afterPrivateVars").asInt());
  }

  /** The value of a named condition in the construct's n-th wrapping rule. */
  private int condition(String construct, int rule, String cond) {
    for (JsonNode candidate : config.at("/wrapping/" + construct + "/rules").get(rule).get("conditions")) {
      if (cond.equals(candidate.get("cond").asString())) return candidate.get("value").asInt();
    }
    throw new AssertionError(construct + " rule " + rule + " has no condition " + cond);
  }
}
