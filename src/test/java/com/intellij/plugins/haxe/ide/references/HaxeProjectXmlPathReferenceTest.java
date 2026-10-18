package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("References: project xml paths")
public class HaxeProjectXmlPathReferenceTest extends HaxeLightFixtureTestCase {

  /** The attribute at the caret carries a path reference that resolves to nothing. */
  private static final String UNRESOLVED = "<unresolved>";
  /** The attribute at the caret carries no path reference. */
  private static final String NO_REFERENCE = "<no reference>";

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  /** (element of the project file with the caret in its path, the target under the project file's folder). */
  static final List<Arguments> PATHS = List.of(
    arguments("<include path=\"de<caret>fs.xml\"/>", "defs.xml"),
    // the legacy name spelling
    arguments("<include name=\"de<caret>fs.xml\"/>", "defs.xml"),
    // an include naming a folder opens the include file Lime picks inside it
    arguments("<include path=\"s<caret>ub\"/>", "sub/include.xml"),
    arguments("<source path=\"s<caret>rc\"/>", "src"),
    arguments("<classpath name=\"s<caret>rc\"/>", "src"),
    arguments("<assets path=\"s<caret>ub\"/>", "sub"),
    arguments("<icon path=\"sub/ic<caret>on.svg\"/>", "sub/icon.svg"),
    arguments("<template path=\"sub/ic<caret>on.svg\"/>", "sub/icon.svg"),
    // never an error: nothing to open stays unresolved
    arguments("<include path=\"mis<caret>sing.xml\"/>", UNRESOLVED),
    // a path Lime substitutes gets no reference
    arguments("<include path=\"${haxelib:lime}/inc<caret>lude.xml\"/>", NO_REFERENCE),
    // a name that is no path
    arguments("<haxedef name=\"de<caret>fs.xml\"/>", NO_REFERENCE),
    arguments("<include haxelib=\"open<caret>fl\"/>", NO_REFERENCE));

  @ParameterizedTest(name = "{0}")
  @FieldSource("PATHS")
  @DisplayName("resolves path attributes against the project file's folder")
  public void testResolvesPathAttributesAgainstTheProjectFilesFolder(String element, String expected) {
    addTargets();
    myFixture.configureByText("project.xml", projectXml(element));

    assertEquals(expected, targetOf(pathReferenceAtCaret()));
  }

  @Test
  @DisplayName("a renamed target renames the path's last segment")
  public void testARenamedTargetRenamesThePathsLastSegment() {
    addTargets();
    myFixture.configureByText("project.xml", projectXml("<icon path=\"sub/ic<caret>on.svg\"/>"));
    HaxeProjectXmlPathReference reference = pathReferenceAtCaret();
    assertNotNull(reference);
    PsiElement icon = reference.resolve();
    assertNotNull(icon);

    myFixture.renameElement(icon, "logo.svg");

    myFixture.checkResult(projectXml("<icon path=\"sub/logo.svg\"/>"));
  }

  @Test
  @DisplayName("ignores xml that is not a project file")
  public void testIgnoresXmlThatIsNotAProjectFile() {
    addTargets();
    myFixture.configureByText("settings.xml", "<config><include path=\"de<caret>fs.xml\"/></config>");

    assertNull(pathReferenceAtCaret());
  }

  @Test
  @DisplayName("resolves paths in a library include file")
  public void testResolvesPathsInALibraryIncludeFile() {
    addTargets();
    myFixture.configureByText("include.xml", "<extension><source path=\"s<caret>rc\"/></extension>");

    assertEquals("src", targetOf(pathReferenceAtCaret()));
  }

  private void addTargets() {
    myFixture.addFileToProject("defs.xml", "<project/>");
    myFixture.addFileToProject("sub/include.xml", "<project/>");
    myFixture.addFileToProject("sub/icon.svg", "<svg/>");
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
  }

  private static String projectXml(String element) {
    return """
      <project>
        <haxelib name="openfl"/>
        %s
      </project>
      """.formatted(element);
  }

  @Nullable
  private HaxeProjectXmlPathReference pathReferenceAtCaret() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    XmlAttributeValue value = PsiTreeUtil.getParentOfType(leaf, XmlAttributeValue.class);
    assertNotNull(value, "the caret sits in an attribute value");
    return Arrays.stream(value.getReferences())
      .filter(HaxeProjectXmlPathReference.class::isInstance)
      .map(HaxeProjectXmlPathReference.class::cast)
      .findFirst()
      .orElse(null);
  }

  /** The reference's target as a path under the project file's folder, or the marker for an unresolved or absent reference. */
  private static String targetOf(@Nullable HaxeProjectXmlPathReference reference) {
    if (reference == null) return NO_REFERENCE;
    if (!(reference.resolve() instanceof PsiFileSystemItem target)) return UNRESOLVED;
    VirtualFile folder = reference.getElement().getContainingFile().getVirtualFile().getParent();
    return VfsUtilCore.getRelativePath(target.getVirtualFile(), folder);
  }
}
