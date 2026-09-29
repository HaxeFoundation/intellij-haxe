package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The expression-evaluation cache is keyed on (element, resolver key); a key
 * COLLISION between two bindings that evaluate differently silently serves
 * the wrong cached type. These tests pin the collision-prone spots of
 * {@link SpecificTypeReference#toCacheKey()} / {@link HaxeGenericResolver#toCacheString()}:
 * qualified names, declaration anchors for type parameters and anonymous
 * structures (whose display NAMES do collide), the assign-hint exclusion,
 * and cycle termination.
 */
@DisplayName("Type model: evaluation cache key")
public class HaxeEvaluationCacheKeyTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    // all fixtures are built inline; no test data directory involved
    return "/";
  }

  @Test
  @DisplayName("same short name in different packages gets distinct keys")
  public void testSameShortNameInDifferentPackagesGetsDistinctKeys() {
    HaxeClass first = addClass("first/Sample.hx", "package first;\nclass Sample {}\n");
    HaxeClass second = addClass("second/Sample.hx", "package second;\nclass Sample {}\n");

    String firstKey = classType(first).toCacheKey();
    String secondKey = classType(second).toCacheKey();

    assertNotEquals(firstKey, secondKey);
    assertTrue(firstKey.contains("first.Sample"), firstKey);
    assertTrue(secondKey.contains("second.Sample"), secondKey);
  }

  @Test
  @DisplayName("same class built twice gets the same key")
  public void testSameClassBuiltTwiceGetsTheSameKey() {
    HaxeClass clazz = addClass("stable/Sample.hx", "package stable;\nclass Sample {}\n");

    // independent reference instances: the key must come from the class
    // identity, never from instance state
    String firstKey = classType(clazz).toCacheKey();
    String secondKey = classType(clazz).toCacheKey();

    assertEquals(firstKey, secondKey);
  }

  // two anonymous structures identical for well over the 128 chars the display
  // name keeps (getName() ellipsis-shortens) and differing only at the tail
  private static final String LONG_ANONYMOUS_FIELDS =
    "fieldA1:Int, fieldA2:Int, fieldA3:Int, fieldA4:Int, fieldA5:Int, fieldA6:Int, " +
    "fieldB1:Int, fieldB2:Int, fieldB3:Int, fieldB4:Int, fieldB5:Int, fieldB6:Int, ";
  private static final String ANONYMOUS_PAIR_HX = """
    class Holder {
      var first:{ %stail:Int };
      var second:{ %stail:String };
    }
    """.formatted(LONG_ANONYMOUS_FIELDS, LONG_ANONYMOUS_FIELDS);

  @Test
  @DisplayName("anonymous structures differing past the name truncation get distinct keys")
  public void testAnonymousStructuresDifferingPastTheNameTruncationGetDistinctKeys() {
    PsiFile file = myFixture.addFileToProject("anon/Holder.hx", ANONYMOUS_PAIR_HX);
    Collection<HaxeAnonymousType> anons = PsiTreeUtil.findChildrenOfType(file, HaxeAnonymousType.class);
    assertEquals(2, anons.size());
    List<HaxeAnonymousType> pair = List.copyOf(anons);
    SpecificHaxeClassReference first = anonymousType(pair.get(0));
    SpecificHaxeClassReference second = anonymousType(pair.get(1));

    // the truncated display names collide - the cache keys must not
    assertEquals(first.getHaxeClassReference().getName(), second.getHaxeClassReference().getName());
    assertNotEquals(first.toCacheKey(), second.toCacheKey());
  }

  @Test
  @DisplayName("anonymous structures in copied files get distinct keys")
  public void testAnonymousStructuresInCopiedFilesGetDistinctKeys() {
    PsiFile original = myFixture.addFileToProject("copies/Holder.hx", "class Holder { var v:{ tag:Int }; }\n");

    // completion-style file copies have no vfs id, share the file NAME and
    // put the anon at the SAME offset - only the underlying node may differ
    PsiFile firstCopy = (PsiFile)original.copy();
    PsiFile secondCopy = (PsiFile)original.copy();
    String originalKey = anonKey(original);
    String firstCopyKey = anonKey(firstCopy);
    String secondCopyKey = anonKey(secondCopy);

    assertNotEquals(firstCopyKey, secondCopyKey);
    assertNotEquals(originalKey, firstCopyKey);
    assertNotEquals(originalKey, secondCopyKey);
  }

  private static String anonKey(PsiFile file) {
    HaxeAnonymousType anonymous = PsiTreeUtil.findChildOfType(file, HaxeAnonymousType.class);
    assertNotNull(anonymous);
    return anonymousType(anonymous).toCacheKey();
  }

  @Test
  @DisplayName("type parameters with the same name from different owners get distinct keys")
  public void testTypeParametersWithTheSameNameFromDifferentOwnersGetDistinctKeys() {
    PsiFile file = myFixture.addFileToProject("owners/Generics.hx", """
      class One<T> {}
      class Two<T> {}
      """);
    List<HaxeTypeParameterDeclaration> params =
      List.copyOf(PsiTreeUtil.findChildrenOfType(file, HaxeTypeParameterDeclaration.class));
    assertEquals(2, params.size());

    String oneKey = classType(params.get(0)).toCacheKey();
    String twoKey = classType(params.get(1)).toCacheKey();

    // owner-qualified, not a bare "T": One's T and Two's T must not unify
    assertNotEquals(oneKey, twoKey);
    assertTrue(oneKey.endsWith(":T") && oneKey.contains("One"), oneKey);
    assertTrue(twoKey.endsWith(":T") && twoKey.contains("Two"), twoKey);
  }

  @Test
  @DisplayName("resolver keys differ per binding and exclude the assign hint")
  public void testResolverKeysDifferPerBindingAndExcludeTheAssignHint() {
    HaxeClass alpha = addClass("bindings/Alpha.hx", "package bindings;\nclass Alpha {}\n");
    HaxeClass beta = addClass("bindings/Beta.hx", "package bindings;\nclass Beta {}\n");
    HaxeClass box = addClass("bindings/Box.hx", "package bindings;\nclass Box<T> {}\n");
    HaxeTypeParameterDeclaration boxParam = PsiTreeUtil.findChildOfType(box, HaxeTypeParameterDeclaration.class);
    assertNotNull(boxParam);

    HaxeGenericResolver alphaBound = new HaxeGenericResolver();
    alphaBound.add(boxParam, classType(alpha).createHolder());
    HaxeGenericResolver betaBound = new HaxeGenericResolver();
    betaBound.add(boxParam, classType(beta).createHolder());
    assertNotEquals(alphaBound.toCacheString(), betaBound.toCacheString());

    // the assign hint deliberately stays out of the key (see the
    // toCacheString javadoc: the hint leaks into transitively-evaluated
    // declarations, and keying on it changes which inferred type wins) -
    // this pins the exclusion so folding it in is a conscious decision
    HaxeGenericResolver alphaBoundWithHint = new HaxeGenericResolver();
    alphaBoundWithHint.add(boxParam, classType(alpha).createHolder());
    alphaBoundWithHint.setAssignHint(classType(beta).createHolder());
    assertEquals(alphaBound.toCacheString(), alphaBoundWithHint.toCacheString());

    assertEquals("EMPTY", new HaxeGenericResolver().toCacheString());
  }

  @Test
  @DisplayName("function types with different argument types get distinct keys")
  public void testFunctionTypesWithDifferentArgumentTypesGetDistinctKeys() {
    HaxeClass alpha = addClass("fn/Alpha.hx", "package fn;\nclass Alpha {}\n");
    HaxeClass beta = addClass("fn/Beta.hx", "package fn;\nclass Beta {}\n");

    ResultHolder sharedReturn = classType(alpha).createHolder();
    String alphaKey = unaryFunction(alpha, classType(alpha).createHolder(), sharedReturn).toCacheKey();
    String betaKey = unaryFunction(alpha, classType(beta).createHolder(), sharedReturn).toCacheKey();

    assertNotEquals(alphaKey, betaKey);
  }

  @Test
  @DisplayName("self referential specifics terminate")
  public void testSelfReferentialSpecificsTerminate() {
    HaxeClass alpha = addClass("cycle/Alpha.hx", "package cycle;\nclass Alpha {}\n");
    ResultHolder[] specifics = new ResultHolder[1];
    SpecificHaxeClassReference selfReferential =
      SpecificHaxeClassReference.withGenerics(new HaxeClassReference(alpha.getModel(), alpha), specifics);
    specifics[0] = selfReferential.createHolder();

    String key = selfReferential.toCacheKey();

    assertTrue(key.contains("@cycle"), key);
  }

  private HaxeClass addClass(String path, String source) {
    PsiFile file = myFixture.addFileToProject(path, source);
    HaxeClass clazz = PsiTreeUtil.findChildOfType(file, HaxeClass.class);
    assertNotNull(clazz);
    return clazz;
  }

  private static SpecificHaxeClassReference classType(HaxeClass clazz) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(clazz.getModel(), clazz));
  }

  private static SpecificHaxeClassReference anonymousType(HaxeAnonymousType anonymous) {
    return SpecificHaxeAnonymousReference.withGenerics(new HaxeClassReference(anonymous.getModel(), anonymous), (HaxeGenericResolver)null);
  }

  private static SpecificFunctionReference unaryFunction(HaxeClass context, ResultHolder argumentType, ResultHolder returnType) {
    HaxeArgument argument = new HaxeArgument(context, 0, false, false, argumentType, "value");
    return new SpecificFunctionReference(List.of(argument), returnType, (HaxeMethodModel)null, context);
  }
}
