/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.mock.MockDumbService;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.RegexLanguageInjector;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.testFramework.ParsingTestCase;
import org.assertj.core.util.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

abstract public class HaxeParsingTestBase extends ParsingTestCase {
  public HaxeParsingTestBase(String... path) {
    super(getPath(path), HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeLanguage.INSTANCE, new HaxeAstFactory());

    // Work around @NotNull bug down in the test fixture.  Since no InjectedLanguageManager
    // was registered, null was passed to a @NotNull function.  This affected testSimple().
    registerExtensionPoint(Extensions.getArea(myProject), MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME, MockMultiHostInjector.class);
    registerExtensionPoint(Extensions.getArea(null), LanguageInjector.EXTENSION_POINT_NAME, RegexLanguageInjector.class); // Might as well use the real one.
    InjectedLanguageManagerImpl manager = new InjectedLanguageManagerImpl(myProject,
                                                                           new MockDumbService(myProject));
    myProject.registerService(InjectedLanguageManager.class, manager);
    // End workaround.
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      if (result.length() > 0) {
        result.append("/");
      }
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }

  public static class MockMultiHostInjector implements MultiHostInjector {
    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {

    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
      return Lists.emptyList();
    }
  }
}
