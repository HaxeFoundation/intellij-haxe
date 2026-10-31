package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerNavigationService.DefinitionKey;
import com.intellij.psi.PsiDocumentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Compiler services: kept go-to-declaration answers")
public class HaxeCompilerNavigationServiceTest extends HaxeLightFixtureTestCase {
  private static final DefinitionKey KEY = new DefinitionKey("context", 42);
  private static final List<Location> TARGET = List.of(new Location("file:///src/Foo.hx", new Range(new Position(0, 6), new Position(0, 9))));

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The service is shared by every test of the light project. */
  @AfterEach
  void forgetAnswers() {
    navigation().clearCaches();
  }

  @Test
  @DisplayName("an answer is kept for its file, context and offset")
  public void testAnAnswerIsKeptForItsFileContextAndOffset() {
    myFixture.configureByText("Main.hx", "class Main {}");
    DefinitionKey elsewhere = new DefinitionKey("context", 43);

    navigation().keepDefinition(myFixture.getFile(), KEY, TARGET);
    navigation().keepDefinition(myFixture.getFile(), elsewhere, List.of());

    assertEquals(TARGET, navigation().keptDefinition(myFixture.getFile(), KEY));
    assertEquals(List.of(), navigation().keptDefinition(myFixture.getFile(), elsewhere), "an empty answer is an answer too");
    assertNull(navigation().keptDefinition(myFixture.getFile(), new DefinitionKey("other context", 42)));
  }

  @Test
  @DisplayName("a psi change retires the answers")
  public void testAPsiChangeRetiresTheAnswers() {
    myFixture.configureByText("Main.hx", "class Main {}");
    navigation().keepDefinition(myFixture.getFile(), KEY, TARGET);

    WriteCommandAction.runWriteCommandAction(getProject(), () -> myFixture.getEditor().getDocument().insertString(0, "\n"));
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    assertNull(navigation().keptDefinition(myFixture.getFile(), KEY), "the targets' ranges may have moved");
  }

  @Test
  @DisplayName("clearing the compiler caches retires the answers")
  public void testClearingTheCompilerCachesRetiresTheAnswers() {
    myFixture.configureByText("Main.hx", "class Main {}");
    navigation().keepDefinition(myFixture.getFile(), KEY, TARGET);

    navigation().clearCaches();

    assertNull(navigation().keptDefinition(myFixture.getFile(), KEY));
  }

  private HaxeCompilerNavigationService navigation() {
    return HaxeCompilerNavigationService.getInstance(getProject());
  }
}
