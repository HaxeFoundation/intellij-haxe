package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
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
 * Checks the chain rule constant that is no setting against the tool's own
 * defaults; every mapped default is guarded by the settings-image round trip
 * in HxformatImportTest. The fixture is what
 * {@code haxelib run formatter --default-config <file>} writes; the file must
 * exist before the command runs. Regenerate it when upgrading the formatter.
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
  @DisplayName("keep item count matches the operator chain rules")
  public void testKeepItemCountMatchesTheOperatorChainRules() {
    assertEquals(HaxeOperatorChainRules.KEEP_ITEM_COUNT, condition("opBoolChain", 2, "itemCount <= n"));
    assertEquals(HaxeOperatorChainRules.KEEP_ITEM_COUNT, condition("opAddSubChain", 2, "itemCount <= n"));
  }

  /** The value of a named condition in the construct's n-th wrapping rule. */
  private int condition(String construct, int rule, String cond) {
    for (JsonNode candidate : config.at("/wrapping/" + construct + "/rules").get(rule).get("conditions")) {
      if (cond.equals(candidate.get("cond").asString())) return candidate.get("value").asInt();
    }
    throw new AssertionError(construct + " rule " + rule + " has no condition " + cond);
  }
}
