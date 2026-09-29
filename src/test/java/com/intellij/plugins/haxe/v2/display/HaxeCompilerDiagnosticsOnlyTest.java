package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedTypeInspection;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project keeps project-level state between tests
      HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
      settings.setCompilerDiagnosticsEnabled(false);
      settings.setCompilerDiagnosticsOnly(false);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("the plugins static analysis stands down while the compiler is the only source")
  public void testThePluginsStaticAnalysisStandsDownWhileTheCompilerIsTheOnlySource() {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerDiagnosticsEnabled(true);
    settings.setCompilerDiagnosticsOnly(true);
    myFixture.configureByText("Main.hx", SOURCE);

    List<HighlightInfo> problems = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING);

    assertTrue(problems.isEmpty(), "no static-analysis problem may show: " + problems);
  }

  @Test
  @DisplayName("static analysis stays while the only toggle is off")
  public void testStaticAnalysisStaysWhileTheOnlyToggleIsOff() {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerDiagnosticsEnabled(true);
    settings.setCompilerDiagnosticsOnly(false);
    myFixture.configureByText("Main.hx", SOURCE);

    List<HighlightInfo> problems = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING);

    assertFalse(problems.isEmpty(), "the unresolved type and call must be flagged by the plugin");
  }

  @Test
  @DisplayName("the only toggle needs the master toggle")
  public void testTheOnlyToggleNeedsTheMasterToggle() {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerDiagnosticsEnabled(false);
    settings.setCompilerDiagnosticsOnly(true);
    myFixture.configureByText("Main.hx", SOURCE);

    List<HighlightInfo> problems = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING);

    assertFalse(settings.isStaticAnalysisSuppressed());
    assertFalse(problems.isEmpty(), "without compiler diagnostics the plugin keeps analysing");
  }
}
