package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Haxelib: default SDK")
public class HaxelibSdkUtilsTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("the default SDK finds its std roots off the UI thread")
  public void testTheDefaultSdkFindsItsStdRootsOffTheUiThread() {
    String home = HaxeSdkUtil.suggestHomePath();
    assumeTrue(home != null && HaxeSdkUtil.testHaxeSdk(home) != null, "no local haxe - skipping");
    // the IDE reports slow operations on the UI thread; unit tests only when asked
    Registry.get("ide.slow.operations.assertion.other").setValue(true, getTestRootDisposable());
    System.setProperty("intellij.idea.forbid.slow.ops", "true");
    Disposer.register(getTestRootDisposable(), () -> System.clearProperty("intellij.idea.forbid.slow.ops"));

    Sdk sdk = HaxelibSdkUtils.createDefaultSdk(home);
    PlatformTestUtil.waitWithEventsDispatching("the default SDK never got its std roots",
                                               () -> sdk.getRootProvider().getFiles(OrderRootType.SOURCES).length > 0, 30);

    assertTrue(sdk.getRootProvider().getFiles(OrderRootType.CLASSES).length > 0);
  }
}
