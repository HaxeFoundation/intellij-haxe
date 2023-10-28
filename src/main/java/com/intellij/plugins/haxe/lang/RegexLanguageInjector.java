/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeRegularExpression;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.lang.regexp.ecmascript.EcmaScriptRegexpLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class RegexLanguageInjector implements LanguageInjector {
  // Same name as LanguageInjector uses.  Duplicated for testing because registerExtensionPoint got more restrictive.
  public static final ExtensionPointName<RegexLanguageInjector> EXTENSION_POINT_NAME = ExtensionPointName.create("com.intellij.languageInjector");

  @Override
  public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar) {
    if (host instanceof HaxeRegularExpression) {
      final String text = host.getText();
      final TextRange textRange = new TextRange(text.indexOf('/') + 1, text.lastIndexOf('/'));
      injectionPlacesRegistrar.addPlace(EcmaScriptRegexpLanguage.INSTANCE, textRange, null, null);
    }
  }
}
