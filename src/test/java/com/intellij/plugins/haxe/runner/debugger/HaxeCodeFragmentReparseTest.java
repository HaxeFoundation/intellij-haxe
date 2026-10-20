package com.intellij.plugins.haxe.runner.debugger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

/**
 * Reparse of the debugger's expression fragments (Evaluate, Watches, Set
 * Value): TYPING in those editors edits the fragment's document, and the
 * commit reparses the fragment through its HAXE_CODE_FRAGMENT element type.
 * On that path the platform hands the parser a FRESH chameleon inside a
 * DummyHolder — an element with NO PSI bound; asking it for its psi went
 * through HaxeParserDefinition.createElement, which has no case for the
 * fragment type: "AssertionError: Unknown element type: HAXE_CODE_FRAGMENT".

 */
@DisplayName("Debugger: code fragment reparse")
public class HaxeCodeFragmentReparseTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private PsiFile fragment(String text) {
    return HaxeElementGenerator.createExpressionCodeFragment(getProject(), text, null, true);
  }

  @Test
  @DisplayName("typing into a fragment reparses and keeps it alive")
  public void testTypingIntoAFragmentReparsesAndKeepsItAlive() {
    PsiFile fragment = fragment("a");
    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(fragment);
    assertNotNull(document);

    // simulate keystrokes: each edit commits like the debugger editors do
    for (String typed : new String[]{".", "b", " ", "+", " ", "c"}) {
      typeInto(document, typed);
    }

    assertEquals("a.b + c", fragment.getText());
    assertNotNull(fragment.getFirstChild(), "reparsed fragment still has a parsed tree");
    assertTrue(fragment.isValid(), "fragment survives repeated reparses");
  }

  /** One committed keystroke, as the debugger editors deliver them. */
  private void typeInto(Document document, String typed) {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      document.insertString(document.getTextLength(), typed);
      PsiDocumentManager.getInstance(getProject()).commitDocument(document);
    });
  }
}
