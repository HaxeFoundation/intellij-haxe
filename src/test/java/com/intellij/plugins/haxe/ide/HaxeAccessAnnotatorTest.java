package com.intellij.plugins.haxe.ide;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

/** Access control by keyword, by metadata, and by @:allow metadata over the fixtures of the accesscontrol package. */
@DisplayName("Annotation: access annotator")
public class HaxeAccessAnnotatorTest extends HaxeSemanticAnnotatorTestBase {

  /** Fixtures declaring {@code package accesscontrol}, so their folder is part of the path. */
  private static final String ALLOW_ACCESS_FOLDER = "accesscontrol/";
  private static final String PRIVATE_STATIC_MEMBERS_HX_NAME = "PrivateStaticMembers.hx";

  @Override
  protected String getBasePath() {
    return "/annotation.access/";
  }

  // KEYWORD access control
  @Test
  @DisplayName("access modifiers static access")
  public void testAccessModifiersStaticAccess() throws Exception {
    doTestNoFixWithWeakWarnings();
  }

  @Test
  @DisplayName("access modifiers instance access")
  public void testAccessModifiersInstanceAccess() throws Exception {
    doTestNoFixWithWeakWarnings();
  }

  @Test
  @DisplayName("access modifiers chain access")
  public void testAccessModifiersChainAccess() throws Exception {
    doTestNoFixWithWeakWarnings();
  }

  // METADATA access control
  @Test
  @DisplayName("meta private access")
  public void testMetaPrivateAccess() throws Exception {
    doTestNoFixWithWeakWarnings();
  }

  // @:allow access control
  @Test
  @DisplayName("static access")
  public void testStaticAccess() throws Exception {
    doAllowAccessTest();
  }

  @Test
  @DisplayName("meta access")
  public void testMetaAccess() throws Exception {
    doAllowAccessTest("AccessMetaTestClass.hx");
  }

  @Test
  @DisplayName("test method allow on property")
  public void testTestMethodAllowOnProperty() throws Exception {
    doAllowAccessTest(PRIVATE_STATIC_MEMBERS_HX_NAME);
  }

  @Test
  @DisplayName("test class allow on field")
  public void testTestClassAllowOnField() throws Exception {
    doAllowAccessTest(PRIVATE_STATIC_MEMBERS_HX_NAME);
  }

  @Test
  @DisplayName("test class allow on class")
  public void testTestClassAllowOnClass() throws Exception {
    doAllowAccessTest(PRIVATE_STATIC_MEMBERS_HX_NAME);
  }

  @Test
  @DisplayName("test module level")
  public void testTestModuleLevel() throws Exception {
    doAllowAccessTest(PRIVATE_STATIC_MEMBERS_HX_NAME);
  }

  /** The test-named fixture and {@code additionalFiles}, all inside the accesscontrol folder. */
  private void doAllowAccessTest(String... additionalFiles) throws Exception {
    String[] files = Stream.concat(Stream.of(getTestName(false) + ".hx"), Arrays.stream(additionalFiles))
      .map(file -> ALLOW_ACCESS_FOLDER + file)
      .toArray(String[]::new);
    doTestFilesNoFixWithWeakWarnings(files);
  }
}
