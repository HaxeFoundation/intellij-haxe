package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("References: project xml haxelib name")
public class HaxeProjectXmlLibraryReferenceTest extends HaxeCodeInsightFixtureTestCase {

  @TempDir
  Path libraryDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The library sync a project open starts would replace the haxelib entries the tests attach. */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    awaitLibrarySync();
  }

  @Test
  @DisplayName("a synced library resolves to its root")
  public void testASyncedLibraryResolvesToItsRoot() throws IOException {
    VirtualFile root = syncedLibrary("somelib");
    myFixture.configureByText("project.xml", projectXml("<haxelib name=\"some<caret>lib\"/>"));

    PsiElement target = libraryReferenceAtCaret().resolve();

    assertEquals(root, assertInstanceOf(PsiDirectory.class, target).getVirtualFile());
  }

  @Test
  @DisplayName("a library that is not synced stays unresolved")
  public void testALibraryThatIsNotSyncedStaysUnresolved() {
    myFixture.configureByText("project.xml", projectXml("<haxelib name=\"other<caret>lib\"/>"));

    assertNull(libraryReferenceAtCaret().resolve());
  }

  @Test
  @DisplayName("names that are no library get no reference")
  public void testNamesThatAreNoLibraryGetNoReference() {
    myFixture.configureByText("project.xml", projectXml("""
      <haxelib name="${LIB<caret>NAME}"/>
      <haxedef name="somelib"/>
      """));

    assertNull(libraryReferenceAt("${LIBNAME}"));
    assertNull(libraryReferenceAt("\"somelib\""));
  }

  @Test
  @DisplayName("renaming the library's folder keeps the name")
  public void testRenamingTheLibrarysFolderKeepsTheName() throws IOException {
    syncedLibrary("somelib");
    String projectXml = projectXml("<haxelib name=\"some<caret>lib\"/>");
    myFixture.configureByText("project.xml", projectXml);
    PsiElement folder = libraryReferenceAtCaret().resolve();
    assertNotNull(folder);

    myFixture.renameElement(folder, "renamed");

    myFixture.checkResult(projectXml);
  }

  /** Attaches a synced haxelib entry whose root is a folder named after the library. */
  private VirtualFile syncedLibrary(String name) throws IOException {
    Path root = Files.createDirectories(libraryDirectory.resolve(name));
    Files.writeString(root.resolve("Lib.hx"), "class Lib {}");
    VirtualFile rootFile = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root));
    ModuleRootModificationUtil.addModuleLibrary(myFixture.getModule(), "haxelib: " + name + " 1.0.0", List.of(rootFile.getUrl()), List.of());
    return rootFile;
  }

  private static String projectXml(String elements) {
    return """
      <project>
        <app main="Main"/>
        %s
      </project>
      """.formatted(elements);
  }

  private HaxeProjectXmlLibraryReference libraryReferenceAtCaret() {
    HaxeProjectXmlLibraryReference reference = libraryReferenceAtOffset(myFixture.getCaretOffset());
    assertNotNull(reference);
    return reference;
  }

  @Nullable
  private HaxeProjectXmlLibraryReference libraryReferenceAt(String text) {
    return libraryReferenceAtOffset(myFixture.getFile().getText().indexOf(text) + 1);
  }

  @Nullable
  private HaxeProjectXmlLibraryReference libraryReferenceAtOffset(int offset) {
    XmlAttributeValue value = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(offset), XmlAttributeValue.class);
    assertNotNull(value, "the offset sits in an attribute value");
    return Arrays.stream(value.getReferences())
      .filter(HaxeProjectXmlLibraryReference.class::isInstance)
      .map(HaxeProjectXmlLibraryReference.class::cast)
      .findFirst()
      .orElse(null);
  }
}
