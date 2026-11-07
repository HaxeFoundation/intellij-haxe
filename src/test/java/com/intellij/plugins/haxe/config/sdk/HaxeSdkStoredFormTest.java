package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** A stored SDK entry saves back as it was read until its values change, whichever plugin version wrote it. */
@DisplayName("SDK: stored form")
public class HaxeSdkStoredFormTest extends HaxeLightFixtureTestCase {

  private static final String STORED_SDK_NAME = "stored-haxe";
  private static final String PLUGIN_1_8_ADDITIONAL = """
    <additional>
      <option name="haxelibPath" value="/opt/haxe/haxelib" />
      <option name="nekoBinPath" value="/opt/neko/neko" />
      <option name="removeCompletionDuplicatesFlag" value="true" />
      <option name="useCompilerCompletionFlag" value="false" />
    </additional>""";

  private final List<Sdk> registeredSdks = new ArrayList<>();

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The fixture's SDK leak check runs before the test root is disposed, so the SDKs go explicitly. */
  @Override
  protected void tearDown() throws Exception {
    try {
      WriteAction.run(() -> registeredSdks.forEach(ProjectJdkTable.getInstance()::removeJdk));
    }
    finally {
      super.tearDown();
    }
  }

  /** (plugin version that wrote the entry, stored additional data). */
  static final List<Arguments> OLDER_ENTRIES = List.of(
    arguments("1.8", PLUGIN_1_8_ADDITIONAL),
    arguments("1.9.0", """
      <additional>
        <option name="haxelibPath" value="/opt/haxe/haxelib" />
        <option name="hlBinPath" value="" />
        <option name="nekoBinPath" value="/opt/neko/neko" />
        <option name="removeCompletionDuplicatesFlag" value="true" />
        <option name="useCompilerCompletionFlag" value="false" />
      </additional>"""),
    // 2.0.0 left the runtime paths out while empty
    arguments("2.0.0", """
      <additional>
        <option name="haxelibPath" value="/opt/haxe/haxelib" />
        <option name="hlBinPath" value="" />
        <option name="nekoBinPath" value="/opt/neko/neko" />
        <option name="removeCompletionDuplicatesFlag" value="true" />
      </additional>"""));

  @ParameterizedTest(name = "{0}")
  @FieldSource("OLDER_ENTRIES")
  @DisplayName("older entries save back unchanged")
  public void testOlderEntriesSaveBackUnchanged(String pluginVersion, String storedAdditional) throws Exception {
    Element stored = JDOMUtil.load(storedAdditional);

    HaxeSdkData data = load(stored);

    assertEquals("/opt/haxe/haxelib", data.getHaxelibPath(), pluginVersion);
    assertEquals(JDOMUtil.write(stored), JDOMUtil.write(save(data)), pluginVersion);
  }

  @Test
  @DisplayName("an entry written now saves back unchanged")
  public void testAnEntryWrittenNowSavesBackUnchanged() {
    HaxeSdkData written = new HaxeSdkData("/opt/haxe", "4.3.7");
    written.setHaxelibPath("/opt/haxe/haxelib");
    Element stored = save(written);

    HaxeSdkData data = load(stored);

    assertEquals("", optionValue(stored, "nodeBinPath"), "the current form writes every option");
    assertEquals(JDOMUtil.write(stored), JDOMUtil.write(save(data)));
  }

  @Test
  @DisplayName("a changed entry saves in the current form")
  public void testAChangedEntrySavesInTheCurrentForm() throws Exception {
    HaxeSdkData data = load(JDOMUtil.load(PLUGIN_1_8_ADDITIONAL));

    data.setNodeBinPath("/opt/node/node");
    Element saved = save(data);

    assertEquals("/opt/node/node", optionValue(saved, "nodeBinPath"));
    assertEquals("/opt/neko/neko", optionValue(saved, "nekoBinPath"));
    assertEquals("", optionValue(saved, "flexSdkName"), "the current form writes every option");
  }

  @Test
  @DisplayName("a registered older sdk saves back unchanged")
  public void testARegisteredOlderSdkSavesBackUnchanged() throws Exception {
    registerStoredSdk(PLUGIN_1_8_ADDITIONAL);
    Sdk sdk = ProjectJdkTable.getInstance().findJdk(STORED_SDK_NAME);
    assertNotNull(sdk);

    HaxeSdkData data = assertInstanceOf(HaxeSdkData.class, sdk.getSdkAdditionalData());

    // the platform compares exactly these on every save
    assertEquals(JDOMUtil.write(JDOMUtil.load(PLUGIN_1_8_ADDITIONAL)), JDOMUtil.write(save(data)));
    assertEquals("/opt/neko/neko", data.getNekoBinPath());
  }

  private static HaxeSdkData load(Element storedAdditional) {
    return assertInstanceOf(HaxeSdkData.class, HaxeSdkType.getInstance().loadAdditionalData(storedAdditional));
  }

  private static Element save(HaxeSdkData data) {
    Element saved = new Element("additional");
    HaxeSdkType.getInstance().saveAdditionalData(data, saved);
    return saved;
  }

  private static String optionValue(Element additional, String name) {
    Element option = additional.getChildren("option").stream()
      .filter(candidate -> name.equals(candidate.getAttributeValue("name")))
      .findFirst()
      .orElse(null);
    assertNotNull(option, "no option " + name);
    return option.getAttributeValue("value");
  }

  /** Registers an SDK read from a jdk table entry, the way the IDE loads one at startup. */
  private void registerStoredSdk(String storedAdditional) throws Exception {
    Element entry = JDOMUtil.load("""
      <jdk version="2">
        <name value="%s" />
        <type value="%s" />
        <version value="4.3.7" />
        <homePath value="/opt/haxe" />
        <roots>
          <classPath><root type="composite" /></classPath>
          <sourcePath><root type="composite" /></sourcePath>
        </roots>
        %s
      </jdk>""".formatted(STORED_SDK_NAME, HaxeSdkType.getInstance().getName(), storedAdditional));
    ProjectJdkImpl sdk = new ProjectJdkImpl(STORED_SDK_NAME, HaxeSdkType.getInstance());
    sdk.readExternal(entry);

    WriteAction.run(() -> ProjectJdkTable.getInstance().addJdk(sdk));
    registeredSdks.add(sdk);
  }
}
