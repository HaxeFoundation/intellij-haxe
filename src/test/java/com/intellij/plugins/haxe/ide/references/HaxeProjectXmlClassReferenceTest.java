package com.intellij.plugins.haxe.ide.references;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("References: project xml main class")
public class HaxeProjectXmlClassReferenceTest extends HaxeLightFixtureTestCase {

  /** The attribute at the caret carries a class reference that resolves to nothing. */
  private static final String UNRESOLVED = "<unresolved>";
  /** The attribute at the caret carries no class reference. */
  private static final String NO_REFERENCE = "<no reference>";

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  /** (element of the project file with the caret in a class name, the qualified name of the target). */
  static final List<Arguments> NAMES = List.of(
    arguments("<app main=\"Ma<caret>in\"/>", "Main"),
    arguments("<app main=\"com.example.Ga<caret>me\"/>", "com.example.Game"),
    // a package segment opens the package
    arguments("<app main=\"com.exa<caret>mple.Game\"/>", "com.example"),
    // never an error: nothing to open stays unresolved
    arguments("<app main=\"Mis<caret>sing\"/>", UNRESOLVED),
    // a name Lime substitutes, or one it would pass on with its spaces
    arguments("<app main=\"${APP_MA<caret>IN}\"/>", NO_REFERENCE),
    arguments("<app main=\" Ma<caret>in\"/>", NO_REFERENCE),
    // another attribute of the element
    arguments("<app file=\"Ma<caret>in\"/>", NO_REFERENCE));

  @ParameterizedTest(name = "{0}")
  @FieldSource("NAMES")
  @DisplayName("resolves the main class and its packages")
  public void testResolvesTheMainClassAndItsPackages(String element, String expected) {
    addClasses();
    myFixture.configureByText("project.xml", projectXml(element));

    assertEquals(expected, targetOf(classReferenceAtCaret()));
  }

  @Test
  @DisplayName("renaming the class renames the attribute")
  public void testRenamingTheClassRenamesTheAttribute() {
    addClasses();
    myFixture.configureByText("project.xml", projectXml("<app main=\"com.example.Ga<caret>me\"/>"));
    PsiElement game = resolvedAtCaret();

    myFixture.renameElement(game, "Level");

    myFixture.checkResult(projectXml("<app main=\"com.example.Level\"/>"));
  }

  @Test
  @DisplayName("find usages of the class lists the attribute")
  public void testFindUsagesOfTheClassListsTheAttribute() {
    addClasses();
    myFixture.configureByText("project.xml", projectXml("<app main=\"Ma<caret>in\"/>"));
    PsiElement main = resolvedAtCaret();

    Collection<UsageInfo> usages = myFixture.findUsages(main);

    boolean listed = usages.stream().anyMatch(usage -> usage.getFile() == myFixture.getFile());
    assertTrue(listed, usages.toString());
  }

  @Test
  @DisplayName("ignores xml that is not a project file")
  public void testIgnoresXmlThatIsNotAProjectFile() {
    addClasses();
    myFixture.configureByText("settings.xml", "<config><app main=\"Ma<caret>in\"/></config>");

    assertEquals(NO_REFERENCE, targetOf(classReferenceAtCaret()));
  }

  @Test
  @DisplayName("resolves the main class in a library include file")
  public void testResolvesTheMainClassInALibraryIncludeFile() {
    addClasses();
    myFixture.configureByText("include.xml", "<extension><app main=\"Ma<caret>in\"/></extension>");

    assertEquals("Main", targetOf(classReferenceAtCaret()));
  }

  private void addClasses() {
    myFixture.addFileToProject("Main.hx", "class Main {}");
    myFixture.addFileToProject("com/example/Game.hx", "package com.example;\nclass Game {}");
  }

  private static String projectXml(String element) {
    return """
      <project>
        <haxelib name="openfl"/>
        %s
      </project>
      """.formatted(element);
  }

  @NotNull
  private PsiElement resolvedAtCaret() {
    HaxeProjectXmlClassReference reference = classReferenceAtCaret();
    assertNotNull(reference);
    PsiElement target = reference.resolve();
    assertNotNull(target);
    return target;
  }

  /** The class reference whose segment holds the caret. */
  @Nullable
  private HaxeProjectXmlClassReference classReferenceAtCaret() {
    int caret = myFixture.getCaretOffset();
    PsiElement leaf = myFixture.getFile().findElementAt(caret);
    XmlAttributeValue value = PsiTreeUtil.getParentOfType(leaf, XmlAttributeValue.class);
    assertNotNull(value, "the caret sits in an attribute value");
    int caretInValue = caret - value.getTextRange().getStartOffset();
    return Arrays.stream(value.getReferences())
      .filter(HaxeProjectXmlClassReference.class::isInstance)
      .map(HaxeProjectXmlClassReference.class::cast)
      .filter(reference -> reference.getRangeInElement().containsOffset(caretInValue))
      .findFirst()
      .orElse(null);
  }

  /** The qualified name of the reference's target, or the marker for an unresolved or absent reference. */
  private static String targetOf(@Nullable HaxeProjectXmlClassReference reference) {
    if (reference == null) return NO_REFERENCE;
    return reference.resolve() instanceof PsiQualifiedNamedElement target ? target.getQualifiedName() : UNRESOLVED;
  }
}
