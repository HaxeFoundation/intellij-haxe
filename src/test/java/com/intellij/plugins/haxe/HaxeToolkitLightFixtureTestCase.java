package com.intellij.plugins.haxe;

import com.intellij.testFramework.LightProjectDescriptor;

/**
 * Light-fixture test with the test toolkit's std mounted as a second
 * source root - the light counterpart of the heavy {@code useHaxeToolkit()}
 * setup. Suites without std needs extend {@link HaxeLightFixtureTestCase}
 * instead.
 */
public abstract class HaxeToolkitLightFixtureTestCase extends HaxeLightFixtureTestCase {
  @Override
  protected LightProjectDescriptor lightProjectDescriptor() {
    return HaxeLightProjectDescriptors.WITH_TOOLKIT;
  }
}
