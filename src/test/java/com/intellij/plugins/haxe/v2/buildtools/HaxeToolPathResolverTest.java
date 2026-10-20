package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Build tools: tool path resolver")
public class HaxeToolPathResolverTest extends HaxeRegisteredSdkTestBase {

  /** The settings belong to the shared light project. */
  @Override
  protected void tearDown() throws Exception {
    try {
      HaxeBuildToolSettings.getInstance(getProject()).setHaxelibPath("");
    }
    finally {
      super.tearDown();
    }
  }

  /** The SDK's configured haxelib wins over the copy in its home; the Build Tools override wins over both. */
  @Test
  @DisplayName("haxelib resolves through the override then the SDKs own haxelib")
  public void testHaxelibResolvesThroughTheOverrideThenTheSdksOwnHaxelib() throws IOException {
    Path configured = touch(temporaryDirectory("haxelib-configured").resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));
    Sdk sdk = registerSdk("resolver-sdk", data -> data.setHaxelibPath(configured.toString()));
    touch(Path.of(sdk.getHomePath()).resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));
    assertEquals(configured, resolvedHaxelib(sdk));

    Path override = touch(temporaryDirectory("haxelib-override").resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));
    HaxeBuildToolSettings.getInstance(getProject()).setHaxelibPath(override.getParent().toString());

    assertEquals(override, resolvedHaxelib(sdk));
  }

  @Test
  @DisplayName("without a configured haxelib the copy in the SDK home serves")
  public void testWithoutAConfiguredHaxelibTheCopyInTheSdkHomeServes() throws IOException {
    Sdk sdk = registerSdk("home-sdk", data -> { });
    Path inHome = touch(Path.of(sdk.getHomePath()).resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));

    assertEquals(inHome, resolvedHaxelib(sdk));
  }

  private Path resolvedHaxelib(Sdk sdk) {
    return Path.of(HaxeToolPathResolver.resolveHaxelibExecutable(getProject(), sdk.getName()));
  }
}
