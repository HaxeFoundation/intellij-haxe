package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
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
public class HaxeCodeFragmentReparseTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private PsiFile fragment(String text) {
    return HaxeElementGenerator.createExpressionCodeFragment(getProject(), text, null, true);
  }

  public void testTypingIntoAFragmentReparsesWithoutError() {
    PsiFile fragment = fragment("counter");
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
    Document document = documentManager.getDocument(fragment);
    assertNotNull(document);

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      document.insertString(document.getTextLength(), " + 1");
      documentManager.commitDocument(document);
    });

    assertEquals("counter + 1", fragment.getText());
    assertNotNull("reparsed fragment still has a parsed tree", fragment.getFirstChild());
  }

  public void testRepeatedEditsKeepTheFragmentAlive() {
    PsiFile fragment = fragment("a");
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
    Document document = documentManager.getDocument(fragment);
    assertNotNull(document);

    // simulate keystrokes: each edit commits like the debugger editors do
    for (String typed : new String[]{".", "b", " ", "+", " ", "c"}) {
      WriteCommandAction.runWriteCommandAction(getProject(), () -> {
        document.insertString(document.getTextLength(), typed);
        documentManager.commitDocument(document);
      });
    }

    assertEquals("a.b + c", fragment.getText());
    assertTrue("fragment survives repeated reparses", fragment.isValid());
  }
}
