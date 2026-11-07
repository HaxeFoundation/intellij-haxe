package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Settings: framework pages")
public class HaxeFrameworkConfigurableTest extends HaxeLightFixtureTestCase {

  static final List<Arguments> PAGES = List.of(
    arguments("Lime", (Supplier<HaxeFrameworkConfigurable>)HaxeFrameworkConfigurables.LimePage::new),
    arguments("OpenFL", (Supplier<HaxeFrameworkConfigurable>)HaxeFrameworkConfigurables.OpenflPage::new),
    arguments("NME", (Supplier<HaxeFrameworkConfigurable>)HaxeFrameworkConfigurables.NmePage::new));

  @Override
  protected String getBasePath() {
    return "";
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("PAGES")
  @DisplayName("a page builds from its form and starts unmodified")
  public void testAPageBuildsFromItsFormAndStartsUnmodified(String name, Supplier<HaxeFrameworkConfigurable> page) {
    HaxeFrameworkConfigurable configurable = page.get();

    assertNotNull(configurable.createComponent());
    assertFalse(configurable.isModified());

    configurable.disposeUIResources();
  }
}
