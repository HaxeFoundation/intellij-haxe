package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Copy from one file, paste into another: the imports and usings the pasted code needs are restored. */
@DisplayName("Editor: copy-paste reference")
public class HaxeCopyPasteReferenceTest extends HaxeLightFixtureTestCase {

  /** The extension receiver is a project class: the light project mounts no std, so a String receiver could not resolve. */
  private static final String HOLDER_HX_SOURCE = """
    package pack;
    class Holder {
      public function new() {}
    }
    """;
  private static final String UTIL_HX_SOURCE = """
    package pack;
    import pack.Holder;
    class Util {
      public static function helper(h:Holder):Holder { return h; }
      public function new() {}
    }
    """;
  private static final String TARGET_HX_SOURCE = """
    class Target {
      function run() {
        <caret>
      }
    }
    """;

  private int addImportsOnPaste;

  @Override
  protected String getBasePath() {
    return "/copypaste/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addImportsOnPaste = CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE;
    CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = CodeInsightSettings.YES;
    myFixture.addFileToProject("pack/Holder.hx", HOLDER_HX_SOURCE);
    myFixture.addFileToProject("pack/Util.hx", UTIL_HX_SOURCE);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = addImportsOnPaste;
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a class reference restores its import")
  public void testAClassReferenceRestoresItsImport() {
    String pasted = copyThenPaste("""
      import pack.Util;
      class Main {
        function run() {
          <selection>var u = new Util();</selection>
        }
      }
      """);

    assertTrue(pasted.contains("import pack.Util;"), pasted);
  }

  @Test
  @DisplayName("an extension method call restores its using")
  public void testAnExtensionMethodCallRestoresItsUsing() {
    String pasted = copyThenPaste("""
      import pack.Holder;
      using pack.Util;
      class Main {
        function run() {
          var h = new Holder();
          <selection>h.helper();</selection>
        }
      }
      """);

    assertEquals(List.of("pack.Util.helper"), copiedReferences(), "the copy recorded the extension method");
    assertTrue(pasted.contains("using pack.Util;"), pasted);
  }

  @Test
  @DisplayName("a static import usage restores its import")
  public void testAStaticImportUsageRestoresItsImport() {
    String pasted = copyThenPaste("""
      import pack.Util.helper;
      class Main {
        function run() {
          <selection>var s = helper("x");</selection>
        }
      }
      """);

    assertTrue(pasted.contains("import pack.Util.helper;"), pasted);
  }

  @Test
  @DisplayName("the candidate filter keeps a using-exposed callee that resolves to its static method")
  public void testTheCandidateFilterKeepsAUsingExposedCalleeThatResolvesToItsStaticMethod() {
    myFixture.configureByText("Main.hx", """
      import pack.Holder;
      using pack.Util;
      class Main {
        function run() {
          var h = new Holder();
          h.hel<caret>per();
        }
      }
      """);
    PsiElement atCaret = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeReferenceExpression callee = PsiTreeUtil.getParentOfType(atCaret, HaxeReferenceExpression.class);
    HaxeImportCandidates candidates = HaxeImportCandidates.of((HaxeFile)myFixture.getFile());

    assertTrue(candidates.mayNeedImport(callee), "a using-exposed callee is an import candidate");

    PsiElement resolved = callee.resolve();
    boolean staticMethod = resolved instanceof HaxeMethod method && method.isStatic();
    assertTrue(staticMethod, "resolved to " + resolved);
  }

  @Test
  @DisplayName("a member chain that never resolves adds nothing")
  public void testAMemberChainThatNeverResolvesAddsNothing() {
    String pasted = copyThenPaste("""
      class Main {
        var holder:Dynamic;
        function run() {
          <selection>var v = holder.generated.deeper.field;</selection>
        }
      }
      """);

    assertFalse(pasted.contains("import"), pasted);
    assertEquals(1, StringUtil.getOccurrenceCount(pasted, "holder.generated.deeper.field"), pasted);
  }

  private String copyThenPaste(String source) {
    myFixture.configureByText("Main.hx", source);
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COPY);
    myFixture.configureByText("Target.hx", TARGET_HX_SOURCE);
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE);
    return myFixture.getEditor().getDocument().getText();
  }

  /** The qualified names our processor put on the clipboard with the last copy. */
  private static List<String> copiedReferences() {
    Transferable contents = CopyPasteManager.getInstance().getContents();
    DataFlavor flavor = HaxeReferenceData.getDataFlavor();
    if (contents == null || flavor == null || !contents.isDataFlavorSupported(flavor)) return List.of();
    try {
      HaxeReferenceTransferableData data = (HaxeReferenceTransferableData)contents.getTransferData(flavor);
      return Arrays.stream(data.getData()).map(reference -> reference.qClassName).toList();
    }
    catch (UnsupportedFlavorException | IOException e) {
      throw new AssertionError(e);
    }
  }
}
