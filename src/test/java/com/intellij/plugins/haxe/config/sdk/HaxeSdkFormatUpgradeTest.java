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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** SDK entries stored by older plugin versions are detected at load and rewritten once. */
@DisplayName("SDK: format upgrade")
public class HaxeSdkFormatUpgradeTest extends HaxeLightFixtureTestCase {

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
  public void testOlderEntriesLoadAsOldFormat(String pluginVersion, String storedAdditional) throws Exception {
    HaxeSdkData data = load(JDOMUtil.load(storedAdditional));

    assertTrue(data.isStoredInOldFormat(), pluginVersion);
    assertEquals("/opt/haxe/haxelib", data.getHaxelibPath());
  }

  @Test
  @DisplayName("an entry written now loads as current format")
  public void testAnEntryWrittenNowLoadsAsCurrentFormat() {
    HaxeSdkData written = new HaxeSdkData("/opt/haxe", "4.3.7");
    written.setHaxelibPath("/opt/haxe/haxelib");
    Element stored = new Element("additional");
    HaxeSdkType.getInstance().saveAdditionalData(written, stored);

    HaxeSdkData data = load(stored);

    assertFalse(data.isStoredInOldFormat());
  }

  @Test
  @DisplayName("upgrade rewrites a registered sdk in the current format")
  public void testUpgradeRewritesARegisteredSdkInTheCurrentFormat() throws Exception {
    registerStoredSdk(PLUGIN_1_8_ADDITIONAL);
    Sdk sdk = ProjectJdkTable.getInstance().findJdk(STORED_SDK_NAME);
    assertNotNull(sdk);
    assertTrue(assertInstanceOf(HaxeSdkData.class, sdk.getSdkAdditionalData()).isStoredInOldFormat());

    HaxeSdkFormatUpgrade.rewriteSdksInOldFormat();

    HaxeSdkData rewritten = assertInstanceOf(HaxeSdkData.class, sdk.getSdkAdditionalData());
    assertFalse(rewritten.isStoredInOldFormat(), "the bridge reloads the committed entry; it must match a fresh save");
    assertEquals("/opt/haxe/haxelib", rewritten.getHaxelibPath());
    assertEquals("/opt/neko/neko", rewritten.getNekoBinPath());
  }

  private static HaxeSdkData load(Element storedAdditional) {
    return assertInstanceOf(HaxeSdkData.class, HaxeSdkType.getInstance().loadAdditionalData(storedAdditional));
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
