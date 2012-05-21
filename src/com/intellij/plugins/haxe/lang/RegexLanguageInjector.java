package com.intellij.plugins.haxe.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeRegularExpression;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class RegexLanguageInjector implements LanguageInjector {
  @Override
  public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar) {
    if (host instanceof HaxeRegularExpression) {
      final String text = host.getText();
      final TextRange textRange = new TextRange(text.indexOf('/') + 1, text.lastIndexOf('/'));
      injectionPlacesRegistrar.addPlace(RegExpLanguage.INSTANCE, textRange, null, null);
    }
  }
}
