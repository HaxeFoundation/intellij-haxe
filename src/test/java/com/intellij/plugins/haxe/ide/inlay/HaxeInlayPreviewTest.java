package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayHintsProviderExtensionBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The inlay settings page renders each provider's shipped {@code /inlayProviders/<providerId>/preview.hx};
 * the hint markers in that file must be what the provider registered under that id produces for it.
 */
@DisplayName("Inlay: settings previews")
public class HaxeInlayPreviewTest extends HaxeInlayTestBase {

  @Override
  protected String getBasePath() {
    return "/inlay/";
  }

  // haxe.compiler.types is absent: its preview carries no markers, since its hints need a running compilation server
  @ParameterizedTest(name = "{0} preview")
  @ValueSource(strings = {"haxe.capture.variable", "haxe.enum.extractor", "haxe.field.type", "haxe.local.variable",
    "haxe.loop.type", "haxe.return.type", "haxe.untyped.parameter.type"})
  @DisplayName("shipped preview renders its own hints")
  public void testShippedPreviewRendersItsOwnHints(String providerId) throws Exception {
    doTestPreview(providerId, registeredProvider(providerId));
  }

  private static InlayHintsProvider registeredProvider(String providerId) {
    InlayHintsProviderExtensionBean registration = InlayHintsProviderExtensionBean.Companion.getEP()
      .findFirstSafe(bean -> providerId.equals(bean.getProviderId()));
    assertNotNull(registration, providerId + " is not a registered declarative inlay provider");
    return registration.getInstance();
  }
}
