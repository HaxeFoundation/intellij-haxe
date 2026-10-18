package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedTypeInspection;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler diagnostics: only mode")
public class HaxeCompilerDiagnosticsOnlyTest extends HaxeLightFixtureTestCase {

  /** An unresolved type and an unresolved call: one problem from the inspection base, one from a self-visiting inspection. */
  private static final String SOURCE = """
    class Main {
    	var field:Nope;
    	static function main() {
    		undefinedCall();
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(HaxeUnresolvedTypeInspection.class, HaxeUnresolvedSymbolInspection.class);
  }

  /** (master toggle, only toggle, the plugin's own analysis suppressed). */
  static final List<Arguments> TOGGLES = List.of(
    // the plugins static analysis stands down while the compiler is the only source
    arguments(true, true, true),
    // static analysis stays while the only toggle is off
    arguments(true, false, false),
    // the only toggle needs the master toggle
    arguments(false, true, false),
    arguments(false, false, false));

  @ParameterizedTest(name = "compiler diagnostics {0}, only {1}: suppressed {2}")
  @FieldSource("TOGGLES")
  @DisplayName("static analysis stands down only under both toggles")
  public void testStaticAnalysisStandsDownOnlyUnderBothToggles(boolean enabled, boolean only, boolean suppressed) {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerDiagnosticsEnabled(enabled);
    settings.setCompilerDiagnosticsOnly(only);
    myFixture.configureByText("Main.hx", SOURCE);

    List<HighlightInfo> problems = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING);

    assertEquals(suppressed, problems.isEmpty(), "static-analysis problems: " + problems);
  }
}
