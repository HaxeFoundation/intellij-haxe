package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("References: string literal links")
public class HaxeStringLiteralLinkTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/references/";
  }

  @Test
  @DisplayName("project relative path resolves to the file")
  public void testProjectRelativePathResolvesToTheFile() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    PsiFile source = myFixture.addFileToProject("Main.hx", """
      class Main { static function main() { var p = "assets/data.txt"; } }
      """);

    PsiElement resolved = resolvedFileTarget(firstStringLiteral(source).getReferences());
    assertInstanceOf(PsiFile.class, resolved, "path must resolve to the file");
    assertEquals("data.txt", ((PsiFile)resolved).getName());
  }

  @Test
  @DisplayName("path relative to the containing file resolves")
  public void testPathRelativeToTheContainingFileResolves() {
    myFixture.addFileToProject("sub/notes.txt", "payload");
    PsiFile source = myFixture.addFileToProject("sub/Code.hx", """
      class Code { static final NOTES = "notes.txt"; }
      """);

    PsiElement resolved = resolvedFileTarget(firstStringLiteral(source).getReferences());
    assertInstanceOf(PsiFile.class, resolved, "sibling file must resolve relative to the containing file");
    assertEquals("notes.txt", ((PsiFile)resolved).getName());
  }

  @Test
  @DisplayName("fully qualified class name resolves to the class")
  public void testFullyQualifiedClassNameResolvesToTheClass() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar { public function new() {} public function baz():Void {} }
      """);
    PsiFile source = myFixture.addFileToProject("Main.hx", """
      class Main { static final TYPE = "foo.Bar"; }
      """);

    List<HaxeStringQnameReference> segments = qnameReferences(firstStringLiteral(source));
    PsiElement resolved = segments.getLast().resolve();
    assertInstanceOf(HaxeClass.class, resolved, "the last segment must resolve to the class");
  }

  @Test
  @DisplayName("package segments of a qualified name navigate to their package")
  public void testPackageSegmentsOfAQualifiedNameNavigateToTheirPackage() {
    myFixture.addFileToProject("foo/deep/Baz.hx", """
      package foo.deep;
      class Baz {}
      """);
    PsiFile source = myFixture.addFileToProject("Main.hx", """
      class Main { static final TYPE = "foo.deep.Baz"; }
      """);

    List<HaxeStringQnameReference> segments = qnameReferences(firstStringLiteral(source));

    List<TextRange> ranges = segments.stream().map(PsiReference::getRangeInElement).toList();
    assertEquals(List.of(new TextRange(1, 4), new TextRange(5, 9), new TextRange(10, 13)), ranges, "one reference per segment, after the opening quote");
    assertEquals("foo", qualifiedNameOf(segments.get(0).resolve()), "a package segment resolves to its package");
    assertEquals("foo.deep", qualifiedNameOf(segments.get(1).resolve()), "a nested package segment resolves to the nested package");
    assertInstanceOf(HaxeClass.class, segments.get(2).resolve(), "the class segment resolves to the class");
  }

  @Test
  @DisplayName("fully qualified member name resolves into the class")
  public void testFullyQualifiedMemberNameResolvesIntoTheClass() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar { public function new() {} public function baz():Void {} }
      """);
    PsiFile source = myFixture.addFileToProject("Main.hx", """
      class Main { static final MEMBER = "foo.Bar.baz"; }
      """);

    List<HaxeStringQnameReference> segments = qnameReferences(firstStringLiteral(source));
    assertNotNull(segments.getLast().resolve(), "the member segment must resolve");
  }

  @Test
  @DisplayName("an unresolvable path and plain prose resolve to nothing and paint no link")
  public void testAnUnresolvablePathAndPlainProseResolveToNothingAndPaintNoLink() {
    myFixture.configureByText("Main.hx", """
      class Main {
        static final MISSING = "does/not/exist.txt";
        static final MESSAGE = "hello there, general text";
      }
      """);
    List<HaxeStringLiteralExpression> literals = List.copyOf(PsiTreeUtil.findChildrenOfType(myFixture.getFile(), HaxeStringLiteralExpression.class));
    assertEquals(2, literals.size(), "fixture must contain the path and the prose");

    // segment references ARE attached to both (completion needs them
    // mid-typing) but nothing resolves - and the painting rule follows resolution
    assertNull(resolvedFileTarget(literals.get(0).getReferences()), "an unresolvable path must resolve to nothing");
    assertNull(resolvedFileTarget(literals.get(1).getReferences()), "prose must resolve to nothing");
    assertFalse(painted(HaxeSyntaxHighlighterColors.STRING_FILE_LINK), "neither must paint as a file link");
    assertFalse(painted(HaxeSyntaxHighlighterColors.STRING_CODE_LINK), "neither must paint as a code link");
  }

  @Test
  @DisplayName("interpolated strings are skipped")
  public void testInterpolatedStringsAreSkipped() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    PsiFile source = myFixture.addFileToProject("Main.hx", """
      class Main { static function main() { var dir = "assets"; var p = 'assets/$dir.txt'; } }
      """);

    HaxeStringLiteralExpression interpolated = null;
    for (HaxeStringLiteralExpression literal : PsiTreeUtil.findChildrenOfType(source, HaxeStringLiteralExpression.class)) {
      if (!literal.getShortTemplateEntryList().isEmpty()) interpolated = literal;
    }
    assertNotNull(interpolated, "fixture must contain an interpolated literal");
    assertFalse(hasLinkReference(interpolated.getReferences()), "interpolation has no constant value to link");
  }

  @Test
  @DisplayName("links are painted with their own configurable attributes")
  public void testLinksArePaintedWithTheirOwnConfigurableAttributes() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar {}
      """);
    myFixture.configureByText("Main.hx", """
      class Main { static final P = "assets/data.txt"; static final T = "foo.Bar"; }
      """);

    assertTrue(painted(HaxeSyntaxHighlighterColors.STRING_FILE_LINK), "resolvable path must carry the String-file-link attributes");
    assertTrue(painted(HaxeSyntaxHighlighterColors.STRING_CODE_LINK), "resolvable qname must carry the String-code-link attributes");
  }

  @Test
  @DisplayName("completion after a slash offers the directory's children")
  public void testCompletionAfterASlashOffersTheDirectorysChildren() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    myFixture.addFileToProject("assets/image.png", "payload");
    myFixture.configureByText("Main.hx", """
      class Main { static final P = "assets/<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "completion in the path segment must produce a lookup");
    assertTrue(lookups.contains("data.txt"), "directory children must be suggested, got: " + lookups);
    assertTrue(lookups.contains("image.png"), "directory children must be suggested, got: " + lookups);
  }

  @Test
  @DisplayName("completion mid segment narrows to matching children")
  public void testCompletionMidSegmentNarrowsToMatchingChildren() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    myFixture.addFileToProject("assets/dawn.txt", "payload");
    myFixture.addFileToProject("assets/image.png", "payload");
    myFixture.configureByText("Main.hx", """
      class Main { static final P = "assets/da<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "completion mid segment must produce a lookup");
    assertTrue(lookups.contains("data.txt"), "matching children must be suggested, got: " + lookups);
    assertTrue(lookups.contains("dawn.txt"), "matching children must be suggested, got: " + lookups);
    assertFalse(lookups.contains("image.png"), "a child not matching the typed prefix must be left out, got: " + lookups);
  }

  @Test
  @DisplayName("compiler only completion leaves string literals to the ide")
  public void testCompilerOnlyCompletionLeavesStringLiteralsToTheIde() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar {}
      """);
    myFixture.configureByText("Main.hx", """
      @:native("foo.<caret>")
      class Main {}
      """);
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    HaxeCompletionMode previousMode = settings.getCompletionMode();
    boolean previousFeatures = settings.isCompilerIdeFeaturesEnabled();
    settings.setCompilerIdeFeaturesEnabled(true);
    settings.setCompletionMode(HaxeCompletionMode.COMPILER_ONLY);

    try {
      myFixture.completeBasic();
      List<String> lookups = myFixture.getLookupElementStrings();
      assertNotNull(lookups, "the compiler cannot complete inside a string, so the ide must");
      assertTrue(lookups.contains("Bar"), "types in the package must be suggested, got: " + lookups);
    }
    finally {
      settings.setCompletionMode(previousMode);
      settings.setCompilerIdeFeaturesEnabled(previousFeatures);
    }
  }

  @Test
  @DisplayName("qname completion after a package dot offers types and subpackages")
  public void testQnameCompletionAfterAPackageDotOffersTypesAndSubpackages() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar {}
      """);
    myFixture.addFileToProject("foo/deep/Baz.hx", """
      package foo.deep;
      class Baz {}
      """);
    myFixture.configureByText("Main.hx", """
      class Main { static final T = "foo.<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "qname completion after a dot must produce a lookup");
    assertTrue(lookups.contains("Bar"), "types in the package must be suggested, got: " + lookups);
    assertTrue(lookups.contains("deep"), "subpackages must be suggested, got: " + lookups);
  }

  @Test
  @DisplayName("qname completion after a class dot offers its members")
  public void testQnameCompletionAfterAClassDotOffersItsMembers() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar {
        public var speed:Int;
        public function jump():Void {}
      }
      """);
    myFixture.configureByText("Main.hx", """
      class Main { static final T = "foo.Bar.<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "member completion after the class must produce a lookup");
    assertTrue(lookups.contains("speed"), "fields must be suggested, got: " + lookups);
    assertTrue(lookups.contains("jump"), "methods must be suggested, got: " + lookups);
  }

  @Test
  @DisplayName("qname completion on the first word offers package roots")
  public void testQnameCompletionOnTheFirstWordOffersPackageRoots() {
    myFixture.addFileToProject("foo/Bar.hx", """
      package foo;
      class Bar {}
      """);
    myFixture.configureByText("Main.hx", """
      class Main { static final T = "fo<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "explicit first-word completion must produce a lookup");
    assertTrue(lookups.contains("foo"), "package roots must be suggested, got: " + lookups);
  }

  @Test
  @DisplayName("explicit completion works from the first segment")
  public void testExplicitCompletionWorksFromTheFirstSegment() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    myFixture.addFileToProject("assist.txt", "payload");
    myFixture.configureByText("Main.hx", """
      class Main { static final P = "ass<caret>"; }
      """);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "first-segment completion must produce a lookup");
    assertTrue(lookups.contains("assets"), "directories from the bases must be suggested, got: " + lookups);
    assertTrue(lookups.contains("assist.txt"), "files from the bases must be suggested, got: " + lookups);
  }

  @Test
  @DisplayName("a bare word matching a file completes but never paints")
  public void testABareWordMatchingAFileCompletesButNeverPaints() {
    myFixture.addFileToProject("assets/data.txt", "payload");
    myFixture.configureByText("Main.hx", """
      class Main { static final NAME = "assets"; }
      """);

    assertFalse(painted(HaxeSyntaxHighlighterColors.STRING_FILE_LINK), "a bare word must not light up as a link even when it resolves");
  }

  /** The literal's qualified-name references in segment order. */
  private static List<HaxeStringQnameReference> qnameReferences(HaxeStringLiteralExpression literal) {
    List<HaxeStringQnameReference> references = new ArrayList<>();
    for (PsiReference reference : literal.getReferences()) {
      if (reference instanceof HaxeStringQnameReference name) references.add(name);
    }
    assertFalse(references.isEmpty(), "the literal must carry qualified-name references");
    return references;
  }

  private static String qualifiedNameOf(PsiElement element) {
    assertInstanceOf(PsiPackage.class, element);
    return ((PsiPackage)element).getQualifiedName();
  }

  private HaxeStringLiteralExpression firstStringLiteral(PsiFile file) {
    HaxeStringLiteralExpression literal = PsiTreeUtil.findChildOfType(file, HaxeStringLiteralExpression.class);
    assertNotNull(literal, "test source must contain a string literal");
    return literal;
  }

  private static boolean hasLinkReference(PsiReference[] references) {
    for (PsiReference reference : references) {
      boolean linkKind = reference instanceof HaxeStringFilePathReference
                         || reference instanceof HaxeStringQnameReference
                         || reference instanceof FileReference;
      if (linkKind) return true;
    }
    return false;
  }

  /** What the path resolves to via the LAST segment reference, or null — mirrors the link-painting rule. */
  private static PsiElement resolvedFileTarget(PsiReference[] references) {
    FileReference last = null;
    for (PsiReference reference : references) {
      if (reference instanceof FileReference fileReference) last = fileReference;
    }
    return last != null ? last.resolve() : null;
  }

  /** Whether the current highlighting pass paints any range with the given forced attributes. */
  private boolean painted(TextAttributesKey key) {
    return myFixture.doHighlighting().stream().anyMatch(info -> info.forcedTextAttributesKey == key);
  }
}
