package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Completion: compiler only")
public class HaxeCompilerOnlyCompletionTest extends HaxeLightFixtureTestCase {

  private static final String STATEMENT_SOURCE = """
    class Main {
    	static function main() {
    		tr<caret>
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project keeps project-level state between tests
      HaxeCompilerSettings.getInstance(getProject()).setCompletionMode(HaxeCompletionMode.IDE_AND_COMPILER);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("without a compilation server the IDE stays silent and notifies once")
  public void testWithoutACompilationServerTheIdeStaysSilentAndNotifiesOnce() {
    // no build file in the light project: the file has no compilation server context
    HaxeCompilerSettings.getInstance(getProject()).setCompletionMode(HaxeCompletionMode.COMPILER_ONLY);
    HaxeCompilerCompletionService service = HaxeCompilerCompletionService.getInstance(getProject());
    assertFalse(service.unavailabilityNotifiedForTests());
    myFixture.configureByText("Main.hx", STATEMENT_SOURCE);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    assertTrue(lookups == null || lookups.isEmpty(), "no IDE suggestion may fill in for the compiler: " + lookups);
    assertTrue(service.unavailabilityNotifiedForTests(), "the missing server must be reported");
    assertFalse(service.ensureAvailable(myFixture.getFile().getVirtualFile()), "the file stays unavailable to the compiler");
  }

  @Test
  @DisplayName("ide modes leave the IDE contributors in place")
  public void testIdeModesLeaveTheIdeContributorsInPlace() {
    HaxeCompilerSettings.getInstance(getProject()).setCompletionMode(HaxeCompletionMode.IDE_ONLY);
    myFixture.configureByText("Main.hx", STATEMENT_SOURCE);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    assertTrue(lookups != null && lookups.contains("trace"), "the IDE's own suggestions must be offered: " + lookups);
  }
}
