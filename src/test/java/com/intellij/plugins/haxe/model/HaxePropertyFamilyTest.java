package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Model: property family")
public class HaxePropertyFamilyTest extends HaxeLightFixtureTestCase {

  private static final String BASE_HX_NAME = "Base.hx";
  private static final String BASE_HX_SOURCE = """
    class Base {
    	public var width(get, set):Int;
    	public var count:Int;
    	function get_width():Int return 0;
    	function set_width(value:Int):Int return value;
    	function get_count():Int return 0;
    }
    """;
  private static final String SUB_HX_NAME = "Sub.hx";
  private static final String SUB_HX_SOURCE = """
    class Sub extends Base {
    	override function get_width():Int return 1;
    }
    """;
  private static final String SIZED_HX_NAME = "Sized.hx";
  private static final String SIZED_HX_SOURCE = """
    interface Sized {
    	var width(get, never):Int;
    }
    """;
  private static final String BOX_HX_NAME = "Box.hx";
  private static final String BOX_HX_SOURCE = """
    class Box implements Sized {
    	public var width(get, never):Int;
    	function get_width():Int return 2;
    }
    """;

  private final Map<String, PsiFile> files = new HashMap<>();

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    files.put(BASE_HX_NAME, myFixture.addFileToProject(BASE_HX_NAME, BASE_HX_SOURCE));
    files.put(SUB_HX_NAME, myFixture.addFileToProject(SUB_HX_NAME, SUB_HX_SOURCE));
    files.put(SIZED_HX_NAME, myFixture.addFileToProject(SIZED_HX_NAME, SIZED_HX_SOURCE));
    files.put(BOX_HX_NAME, myFixture.addFileToProject(BOX_HX_NAME, BOX_HX_SOURCE));
  }

  @Test
  @DisplayName("a property gathers its accessors and their overrides")
  public void testAPropertyGathersItsAccessorsAndTheirOverrides() {
    HaxePropertyFamily family = HaxePropertyFamily.of(member(BASE_HX_NAME, "width"));

    assertNotNull(family);
    assertEquals(List.of("Base.width"), idsOf(family.properties()));
    assertEquals(List.of("Base.get_width", "Sub.get_width"), idsOf(family.getters()));
    assertEquals(List.of("Base.set_width"), idsOf(family.setters()));
  }

  @Test
  @DisplayName("an overriding accessor finds the same family")
  public void testAnOverridingAccessorFindsTheSameFamily() {
    HaxePropertyFamily family = HaxePropertyFamily.of(member(SUB_HX_NAME, "get_width"));

    assertNotNull(family);
    assertEquals(List.of("Base.width"), idsOf(family.properties()));
    assertEquals(List.of("Base.get_width", "Sub.get_width"), idsOf(family.getters()));
  }

  @Test
  @DisplayName("an implemented interface property joins the family from either side")
  public void testAnImplementedInterfacePropertyJoinsTheFamilyFromEitherSide() {
    HaxePropertyFamily fromClass = HaxePropertyFamily.of(member(BOX_HX_NAME, "width"));
    HaxePropertyFamily fromInterface = HaxePropertyFamily.of(member(SIZED_HX_NAME, "width"));

    assertNotNull(fromClass);
    assertNotNull(fromInterface);
    assertEquals(List.of("Box.width", "Sized.width"), idsOf(fromClass.properties()));
    assertEquals(List.of("Sized.width", "Box.width"), idsOf(fromInterface.properties()));
    assertEquals(List.of("Box.get_width"), idsOf(fromClass.getters()));
    assertEquals(List.of("Box.get_width"), idsOf(fromInterface.getters()));
  }

  @Test
  @DisplayName("a plain field and an unbound method have no family")
  public void testAPlainFieldAndAnUnboundMethodHaveNoFamily() {
    assertNull(HaxePropertyFamily.of(member(BASE_HX_NAME, "count")));
    assertNull(HaxePropertyFamily.of(member(BASE_HX_NAME, "get_count")));
  }

  @Test
  @DisplayName("renaming the property renames the accessors after it")
  public void testRenamingThePropertyRenamesTheAccessorsAfterIt() {
    HaxeNamedComponent property = member(BASE_HX_NAME, "width");
    HaxePropertyFamily family = HaxePropertyFamily.of(property);
    assertNotNull(family);

    Map<String, String> renames = namesOf(family.renamesFor(property, "size"));

    assertEquals(Map.of("Base.get_width", "get_size", "Sub.get_width", "get_size", "Base.set_width", "set_size"), renames);
  }

  @Test
  @DisplayName("renaming an accessor renames the property behind its prefix")
  public void testRenamingAnAccessorRenamesThePropertyBehindItsPrefix() {
    HaxeNamedComponent getter = member(BASE_HX_NAME, "get_width");
    HaxePropertyFamily family = HaxePropertyFamily.of(getter);
    assertNotNull(family);

    Map<String, String> renames = namesOf(family.renamesFor(getter, "get_size"));
    Map<String, String> withoutPrefix = namesOf(family.renamesFor(getter, "fetch"));

    assertEquals(Map.of("Base.width", "size", "Sub.get_width", "get_size", "Base.set_width", "set_size"), renames);
    assertEquals(Map.of(), withoutPrefix);
  }

  private HaxeNamedComponent member(String fileName, String name) {
    for (HaxeNamedComponent component : PsiTreeUtil.findChildrenOfType(files.get(fileName), HaxeNamedComponent.class)) {
      if (name.equals(component.getName())) return component;
    }
    throw new AssertionError(fileName + " declares no " + name);
  }

  /** {@code Class.member} for each member, in the given order. */
  private static List<String> idsOf(List<? extends PsiNamedElement> members) {
    return members.stream().map(HaxePropertyFamilyTest::idOf).toList();
  }

  private static Map<String, String> namesOf(Map<PsiNamedElement, String> renames) {
    Map<String, String> names = new HashMap<>();
    renames.forEach((member, newName) -> names.put(idOf(member), newName));
    return names;
  }

  private static String idOf(PsiElement member) {
    HaxeClass owner = PsiTreeUtil.getParentOfType(member, HaxeClass.class);
    assertNotNull(owner);
    return owner.getName() + "." + ((PsiNamedElement)member).getName();
  }
}
