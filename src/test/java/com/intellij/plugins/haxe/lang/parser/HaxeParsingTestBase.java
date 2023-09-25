/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019-2020 Eric Bishton
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
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.metadata.HaxeMetadataLanguage;
import com.intellij.plugins.haxe.metadata.parser.HaxeMetadataParserDefinition;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

abstract public class HaxeParsingTestBase extends ParsingTestCase {
  public HaxeParsingTestBase(String... path) {
    super(getPath(path), HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition(), new HaxeMetadataParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    HaxeAstFactory astFactory = new HaxeAstFactory();
    addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeLanguage.INSTANCE, astFactory);
    addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeMetadataLanguage.INSTANCE, astFactory);

    registerMetadataParser();


  }



  private <T> void registerMetadataParser() {
    // Get the metadata parser added because only the first language definition is added by the super.setUp call.
    // This is basically what configureFromParserDefinition does, but without overriding the globals.
    HaxeMetadataParserDefinition metaParser = new HaxeMetadataParserDefinition();
    addExplicitExtension(LanguageParserDefinitions.INSTANCE, HaxeMetadataLanguage.INSTANCE, metaParser);
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
    // Be careful with this.  It is the same extension point name as (and thus will conflict with) MultiHostInjector uses.
    public static final ExtensionPointName<MockMultiHostInjector> MULTIHOST_INJECTOR_EP_NAME =
      ExtensionPointName.create("com.intellij.multiHostInjector");

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {

    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
      return new ArrayList<>();
    }
  }
}
