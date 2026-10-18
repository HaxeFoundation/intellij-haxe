package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryDefinesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeResolveContextStore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The define set a file is lexed with follows its resolve context (its own build, or a pin), not the active build file. */
@DisplayName("Define context: per file")
public class HaxeFileDefineContextTest extends HaxeCodeInsightFixtureTestCase {
  private static final String WEB_HXML_SOURCE = """
    -cp src
    -main WebMain
    -js bin/web.js
    """;
  private static final String CPP_HXML_SOURCE = """
    -cp src
    -main CppMain
    -cpp bin/cpp
    """;
  /** Both branches declare the class; which one is the dead blob tells the contexts apart. */
  private static final String SYS_GATED_SOURCE = """
    #if sys
    class %1$s { static var sys = true; }
    #else
    class %1$s { static var sys = false; }
    #end
    """;
  private static final String FOO_GATED_SOURCE = """
    #if foo
    class %1$s { static var foo = true; }
    #else
    class %1$s { static var foo = false; }
    #end
    """;
  private static final String LIB_HXML_SOURCE = """
    -cp src
    -main LibMain
    -lib somelib
    --interp
    """;
  private static final String LIB_GATED_SOURCE = """
    #if somelib
    class LibMain { static var somelib = true; }
    #else
    class LibMain { static var somelib = false; }
    #end
    """;

  /** The names of the files each invalidation re-lexed, in order. */
  private final List<String> invalidated = new CopyOnWriteArrayList<>();

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // the library sync a project open starts would replace the library defines a test records
    awaitLibrarySync();
    HaxeDefineContextInvalidator.invalidationObserver = files -> files.forEach(file -> invalidated.add(file.getName()));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      HaxeDefineContextInvalidator.invalidationObserver = null;
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a file is lexed with the defines of its own build, not the active one")
  public void testAFileIsLexedWithTheDefinesOfItsOwnBuildNotTheActiveOne() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE);
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    PsiFile cppMain = myFixture.addFileToProject("src/CppMain.hx", SYS_GATED_SOURCE.formatted("CppMain"));
    PsiFile webMain = myFixture.addFileToProject("src/WebMain.hx", SYS_GATED_SOURCE.formatted("WebMain"));

    assertTrue(branchLive(cppMain, "sys = true"), "CppMain is cpp.hxml's main: sys is defined there while web.hxml is active");
    assertFalse(branchLive(webMain, "sys = true"), "WebMain follows web.hxml, which has no sys");
  }

  @Test
  @DisplayName("a pin re-lexes the file under the pinned builds defines")
  public void testAPinReLexesTheFileUnderThePinnedBuildsDefines() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile shared = myFixture.addFileToProject("src/Shared.hx", SYS_GATED_SOURCE.formatted("Shared")).getVirtualFile();
    myFixture.openFileInEditor(shared);
    assertFalse(branchLive(psiFile(shared), "sys = true"), "a shared file follows the active build before a pin");

    HaxeResolveContextStore.getInstance(getProject()).pin(shared, cpp.getPath());
    HaxeDefineContextService.getInstance(getProject()).relexFile(shared);

    PlatformTestUtil.waitWithEventsDispatching("pinned to cpp.hxml: the sys branch must come alive", () -> branchLive(psiFile(shared), "sys = true"), 10);
  }

  @Test
  @DisplayName("a build file edit re-lexes the files it defines")
  public void testABuildFileEditReLexesTheFilesItDefines() throws IOException {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile cppMain = myFixture.addFileToProject("src/CppMain.hx", FOO_GATED_SOURCE.formatted("CppMain")).getVirtualFile();
    VirtualFile webMain = myFixture.addFileToProject("src/WebMain.hx", FOO_GATED_SOURCE.formatted("WebMain")).getVirtualFile();
    myFixture.openFileInEditor(cppMain);
    assertFalse(branchLive(psiFile(cppMain), "foo = true"), "no -D foo yet");
    Set<String> cppContext = Set.of(cpp.getPath());
    List<VirtualFile> inCpp = HaxeReadActions.compute(() -> HaxeDefineContextInvalidator.inContextOf(getProject(), List.of(cppMain, webMain), cppContext));
    assertEquals(List.of(cppMain), inCpp, "only CppMain is lexed under cpp.hxml");

    WriteAction.runAndWait(() -> VfsUtil.saveText(cpp, CPP_HXML_SOURCE + "-D foo\n"));

    awaitInvalidationOf("CppMain.hx", "cpp.hxml gained -D foo: CppMain must be re-lexed");
    awaitBranchLive(cppMain, "foo = true", "the foo branch is live after the re-lex");
  }

  @Test
  @DisplayName("an unsaved build file edit re-lexes the files it defines on the next refresh")
  public void testAnUnsavedBuildFileEditReLexesTheFilesItDefinesOnTheNextRefresh() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile cppMain = myFixture.addFileToProject("src/CppMain.hx", FOO_GATED_SOURCE.formatted("CppMain")).getVirtualFile();
    myFixture.openFileInEditor(cppMain);
    assertFalse(branchLive(psiFile(cppMain), "foo = true"), "no -D foo yet");

    myFixture.openFileInEditor(cpp);
    Document document = myFixture.getEditor().getDocument();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), "-D foo\n"));
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    HaxeDefineContextService.getInstance(getProject()).refreshAsync();

    awaitInvalidationOf("CppMain.hx", "cpp.hxml gained -D foo in the editor: CppMain must be re-lexed");
    awaitBranchLive(cppMain, "foo = true", "the foo branch is live after the re-lex");
  }

  @Test
  @DisplayName("a read of the new defines before the refresh does not mask the change")
  public void testAReadOfTheNewDefinesBeforeTheRefreshDoesNotMaskTheChange() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile cppMain = myFixture.addFileToProject("src/CppMain.hx", FOO_GATED_SOURCE.formatted("CppMain")).getVirtualFile();
    myFixture.openFileInEditor(cppMain);
    assertFalse(branchLive(psiFile(cppMain), "foo = true"), "no -D foo yet");

    myFixture.openFileInEditor(cpp);
    Document document = myFixture.getEditor().getDocument();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), "-D foo\n"));
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    // what an annotator pass on CppMain does between the edit and the refresh
    Map<String, String> read = HaxeReadActions.compute(() -> HaxeConditionalExpression.definitionsFor(getProject(), psiFile(cppMain)));
    assertTrue(read.containsKey("foo"), "the reader already sees the new define");
    HaxeDefineContextService.getInstance(getProject()).refreshAsync();

    awaitInvalidationOf("CppMain.hx", "the read must not count as lexed under: CppMain is re-lexed");
    awaitBranchLive(cppMain, "foo = true", "the foo branch is live after the re-lex");
  }

  @Test
  @DisplayName("a highlighting pass re-lexes a file whose branches disagree with its defines")
  public void testAHighlightingPassReLexesAFileWhoseBranchesDisagreeWithItsDefines() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile cppMain = myFixture.addFileToProject("src/CppMain.hx", FOO_GATED_SOURCE.formatted("CppMain")).getVirtualFile();
    myFixture.openFileInEditor(cppMain);
    assertFalse(branchLive(psiFile(cppMain), "foo = true"), "no -D foo yet");

    // the edit reaches no refresh: nothing but the highlighting pass can notice it
    myFixture.openFileInEditor(cpp);
    Document document = myFixture.getEditor().getDocument();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), "-D foo\n"));
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    myFixture.openFileInEditor(cppMain);
    // the pass requests a reparse of the file it highlights; the harness refuses that unless told
    ((CodeInsightTestFixtureImpl)myFixture).canChangeDocumentDuringHighlighting(true);
    myFixture.doHighlighting();

    awaitInvalidationOf("CppMain.hx", "the pass saw a dead branch whose condition holds: CppMain is re-lexed");
    awaitBranchLive(cppMain, "foo = true", "the foo branch is live after the re-lex");
  }

  @Test
  @DisplayName("library defines a sync records re-lex the files lexed without them")
  public void testLibraryDefinesASyncRecordsReLexTheFilesLexedWithoutThem() {
    VirtualFile lib = myFixture.addFileToProject("lib.hxml", LIB_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(lib.getPath());
    VirtualFile libMain = myFixture.addFileToProject("src/LibMain.hx", LIB_GATED_SOURCE).getVirtualFile();
    myFixture.openFileInEditor(libMain);
    assertFalse(branchLive(psiFile(libMain), "somelib = true"), "no library defines before the first sync");

    String containerId = HaxeContainers.containerIdFor(getProject(), lib);
    HaxeLibraryDefinesStore.getInstance(getProject()).replaceAll(Map.of(containerId, Map.of("somelib", Map.of("somelib", "1.0.0"))));
    HaxeBuildEventListener.publisher(getProject()).librariesSynced();

    awaitInvalidationOf("LibMain.hx", "the sync recorded somelib=1.0.0: LibMain must be re-lexed");
    awaitBranchLive(libMain, "somelib = true", "the somelib branch is live after the re-lex");
  }

  @Test
  @DisplayName("an IDE define reaches only the files of its own build")
  public void testAnIdeDefineReachesOnlyTheFilesOfItsOwnBuild() {
    VirtualFile web = myFixture.addFileToProject("web.hxml", WEB_HXML_SOURCE).getVirtualFile();
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    VirtualFile cppMain = myFixture.addFileToProject("src/CppMain.hx", FOO_GATED_SOURCE.formatted("CppMain")).getVirtualFile();
    VirtualFile webMain = myFixture.addFileToProject("src/WebMain.hx", FOO_GATED_SOURCE.formatted("WebMain")).getVirtualFile();
    myFixture.openFileInEditor(cppMain);
    assertFalse(branchLive(psiFile(cppMain), "foo = true"), "no foo define yet");

    HaxeIdeOverridesStore.getInstance(getProject()).putDefine(cpp.getPath(), "foo", "");

    awaitBranchLive(cppMain, "foo = true", "cpp.hxml's IDE define foo must reach CppMain");
    assertFalse(branchLive(psiFile(webMain), "foo = true"), "web.hxml has no IDE define: WebMain keeps its foo branch dead");
  }

  @Test
  @DisplayName("a custom target applies only to a build that selects no target")
  public void testACustomTargetAppliesOnlyToABuildThatSelectsNoTarget() {
    VirtualFile cpp = myFixture.addFileToProject("cpp.hxml", CPP_HXML_SOURCE).getVirtualFile();
    VirtualFile extraParams = myFixture.addFileToProject("extraParams.hxml", "-cp src\n").getVirtualFile();
    HaxeIdeOverridesStore overrides = HaxeIdeOverridesStore.getInstance(getProject());

    overrides.setCustomTarget(cpp.getPath(), "go");
    overrides.setCustomTarget(extraParams.getPath(), "go");

    HaxeDefineContextService defineContext = HaxeDefineContextService.getInstance(getProject());
    Map<String, String> cppDefines = defineContext.definesFor(cpp);
    Map<String, String> extraParamsDefines = defineContext.definesFor(extraParams);
    assertNotNull(cppDefines);
    assertNotNull(extraParamsDefines);
    assertNull(cppDefines.get("custom_target"), "cpp.hxml selects -cpp itself, so the custom target does not apply");
    assertNotEquals("go", cppDefines.get("target.name"));
    assertNotNull(extraParamsDefines.get("custom_target"));
    assertEquals("go", extraParamsDefines.get("target.name"));
  }

  private void awaitInvalidationOf(String fileName, String message) {
    PlatformTestUtil.waitWithEventsDispatching(message, () -> invalidated.contains(fileName), 10);
  }

  /** The reparse an invalidation requests lands asynchronously (background re-index, editor reparse on the EDT). */
  private void awaitBranchLive(VirtualFile file, String marker, String message) {
    PlatformTestUtil.waitWithEventsDispatching(message, () -> branchLive(psiFile(file), marker), 10);
  }

  private PsiFile psiFile(VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    assertNotNull(psiFile);
    return psiFile;
  }

  /** Whether the declaration holding the marker sits in live code rather than in a dead-branch blob. */
  private static boolean branchLive(PsiFile file, String marker) {
    PsiElement leaf = file.findElementAt(file.getText().indexOf(marker));
    assertNotNull(leaf);
    return PsiTreeUtil.getParentOfType(leaf, HaxeInactiveBody.class) == null;
  }
}
