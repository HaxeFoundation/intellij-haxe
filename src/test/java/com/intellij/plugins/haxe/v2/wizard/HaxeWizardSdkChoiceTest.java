package com.intellij.plugins.haxe.v2.wizard;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The wizard's SDK choice has to reach the stores v2 reads, not only the Project SDK. */
@DisplayName("Project generator: wizard SDK choice")
public class HaxeWizardSdkChoiceTest extends HaxeLightFixtureTestCase {

  private static final String FIRST_SDK_NAME = "wizard-first";
  private static final String SECOND_SDK_NAME = "wizard-second";

  private final List<Sdk> registeredSdks = new ArrayList<>();
  private Sdk previousProjectSdk;

  @Override
  protected String getBasePath() {
    return "/wizard/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registerHaxeSdk(FIRST_SDK_NAME);
    registerHaxeSdk(SECOND_SDK_NAME);
    previousProjectSdk = ProjectRootManager.getInstance(getProject()).getProjectSdk();
  }

  /**
   * The light project is shared: project-level state written here must not
   * leak, and the fixture's SDK leak check runs before the test root is
   * disposed, so the SDKs go explicitly.
   */
  @Override
  protected void tearDown() throws Exception {
    try {
      HaxeBuildToolSettings.getInstance(getProject()).setSdkName(null);
      WriteAction.run(() -> {
        ProjectRootManager.getInstance(getProject()).setProjectSdk(previousProjectSdk);
        registeredSdks.forEach(ProjectJdkTable.getInstance()::removeJdk);
      });
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("install as project sdk sets the build tools default and the project sdk")
  public void testInstallAsProjectSdkSetsTheBuildToolsDefaultAndTheProjectSdk() {
    Sdk chosen = ProjectJdkTable.getInstance().findJdk(SECOND_SDK_NAME);
    assertNotNull(chosen);

    HaxeProjectSdkStep.installAsProjectSdk(getProject(), chosen);

    assertEquals(SECOND_SDK_NAME, HaxeBuildToolSettings.getInstance(getProject()).getSdkName());
    assertEquals(SECOND_SDK_NAME, sdkName(ProjectRootManager.getInstance(getProject()).getProjectSdk()));
  }

  @Test
  @DisplayName("module builder assigns the build tools default as an explicit module sdk")
  public void testModuleBuilderAssignsTheBuildToolsDefaultAsAnExplicitModuleSdk() {
    HaxeBuildToolSettings.getInstance(getProject()).setSdkName(SECOND_SDK_NAME);
    ModifiableRootModel model = ModuleRootManager.getInstance(myFixture.getModule()).getModifiableModel();

    try {
      HaxeModuleBuilder.assignSdk(model);

      assertFalse(model.isSdkInherited(), "an explicit module SDK, not the Project SDK");
      assertEquals(SECOND_SDK_NAME, sdkName(model.getSdk()));
    }
    finally {
      model.dispose();
    }
  }

  @Test
  @DisplayName("module builder falls back to a registered haxe sdk without a build tools default")
  public void testModuleBuilderFallsBackToARegisteredHaxeSdkWithoutABuildToolsDefault() {
    HaxeBuildToolSettings.getInstance(getProject()).setSdkName(null);
    ModifiableRootModel model = ModuleRootManager.getInstance(myFixture.getModule()).getModifiableModel();

    try {
      HaxeModuleBuilder.assignSdk(model);

      Sdk assigned = model.getSdk();
      assertFalse(model.isSdkInherited(), "an explicit module SDK, not the Project SDK");
      assertNotNull(assigned);
      assertInstanceOf(HaxeSdkType.class, assigned.getSdkType());
    }
    finally {
      model.dispose();
    }
  }

  /** The SDK table is application-wide; tearDown removes what this registers. */
  private void registerHaxeSdk(String name) {
    ProjectJdkTable table = ProjectJdkTable.getInstance();
    Sdk sdk = table.createSdk(name, HaxeSdkType.getInstance());
    WriteAction.run(() -> table.addJdk(sdk));
    registeredSdks.add(sdk);
  }

  private static String sdkName(Sdk sdk) {
    return sdk == null ? null : sdk.getName();
  }
}
